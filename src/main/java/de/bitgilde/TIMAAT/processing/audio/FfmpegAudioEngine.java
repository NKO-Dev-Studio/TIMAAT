package de.bitgilde.TIMAAT.processing.audio;

import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.PropertyManagement;
import de.bitgilde.TIMAAT.processing.ExternalProcessExecutor;
import de.bitgilde.TIMAAT.processing.audio.api.AudioMetaInformation;
import de.bitgilde.TIMAAT.processing.audio.api.Complex;
import de.bitgilde.TIMAAT.processing.audio.api.PcmMono16BitLittleEndian;
import de.bitgilde.TIMAAT.processing.audio.api.WaveformDataPoint;
import de.bitgilde.TIMAAT.processing.audio.exception.AudioEngineException;
import de.bitgilde.TIMAAT.processing.audio.io.FrequencyFileWriter;
import de.bitgilde.TIMAAT.processing.audio.io.WaveformBinaryFileWriter;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage.TemporaryFile;
import jakarta.inject.Inject;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

/**
 * This engine is a wrapper around FFMPEG to give easy access to additional information of provided audio files
 * as well as converting and transforming actions
 *
 * @author Nico Kotlenga
 * @since 25.07.25
 */
public final class FfmpegAudioEngine extends ExternalProcessExecutor {

    private static final Logger logger = Logger.getLogger(FfmpegAudioEngine.class.getName());

    private static final int SAMPLE_RATE = 44100;
    private static final int WAVEFORM_SEGMENT_COUNT = 40000;
    private static final int FREQUENCY_EXTRACTION_INTERVAL_MS = 500;
    private static final int SAMPLES_PER_FREQUENCY_DATA_POINT = SAMPLE_RATE * FREQUENCY_EXTRACTION_INTERVAL_MS / 1000;

    private final Path pathToFfmpeg;
    private final Path pathToFfprobe;
    private final TemporaryFileStorage temporaryFileStorage;

    @Inject
    public FfmpegAudioEngine(PropertyManagement propertyManagement, TemporaryFileStorage temporaryFileStorage) {
        Path baseFfmpegPath = Path.of(propertyManagement.getProp(PropertyConstants.FFMPEG_LOCATION));
        this.pathToFfmpeg = baseFfmpegPath.resolve("ffmpeg");
        this.pathToFfprobe = baseFfmpegPath.resolve("ffprobe");
        this.temporaryFileStorage = temporaryFileStorage;
    }

    public AudioMetaInformation extractAudioMetaInformation(Path pathToAudioFile) throws AudioEngineException {
        logger.log(Level.FINE, "Extracting audio meta data from file {0}", pathToAudioFile);

        String[] commandLine = {pathToFfprobe.toString(), "-select_streams", "a:0", "-show_entries",
                "stream=codec_name,bit_rate,sample_rate,channels,duration", "-v", "quiet", "-of", "json",
                pathToAudioFile.toString()};
        try {
            JSONObject json = executeJsonResponseCommand(commandLine);
            JSONObject metaInformation = json.getJSONArray("streams").getJSONObject(0);

            String codeName = metaInformation.getString("codec_name");
            int sampleRate = metaInformation.getInt("sample_rate");
            int channels = metaInformation.getInt("channels");
            float duration = metaInformation.getFloat("duration");
            int bitRate = metaInformation.getInt("bit_rate");

            return new AudioMetaInformation(codeName, sampleRate, channels, duration, bitRate);
        } catch (Exception e) {
            throw new AudioEngineException("Error during extracting audio meta information", e);
        }
    }

    /**
     * Converts the audio of the given file to a single mono channel of 16 bit little endian PCM samples
     * at the engine's sample rate by invoking ffmpeg. Depending on {@code persistInContainerFormat} the
     * resulting samples are either written as a raw PCM stream ({@code -f s16le}) or wrapped inside a
     * WAV container ({@code -f wav}).
     *
     * @param pathToAudioFile          path to the source audio file
     * @param persistInContainerFormat when {@code true} the converted audio is wrapped inside a WAV
     *                                 container; when {@code false} the samples are persisted as raw
     *                                 16 bit little endian PCM without container metadata
     * @return a handle to the converted audio file; the caller is responsible for closing it
     * @throws AudioEngineException if creating the temporary output file or executing ffmpeg fails
     */
    public PcmMono16BitLittleEndian convertAudioChannelsTo16BitLittleEndian(Path pathToAudioFile, boolean persistInContainerFormat) throws AudioEngineException {
        logger.log(Level.FINE, "Converting file {0} to mono", pathToAudioFile);
        try {
            TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile();
            String outputFormat = persistInContainerFormat ? "wav" : "s16le";
            String[] commandLine = {pathToFfmpeg.toString(), "-i", pathToAudioFile.toString(), "-ac", "1", "-ar", String.valueOf(SAMPLE_RATE), "-f", outputFormat, temporaryFile.getTemporaryFilePath().toString()};
            syncExecuteProcess(commandLine);

            return new PcmMono16BitLittleEndian(temporaryFile);
        } catch (Exception e) {
            throw new AudioEngineException("Error during converting file to mono", e);
        }
    }

    public void createWaveformBinary(PcmMono16BitLittleEndian audioFile, Path waveformBinaryResultPath) throws AudioEngineException {
        logger.log(Level.FINE, "Creating waveform binary for audio file located at {0}", audioFile.getAudioFilePath());
        try (WaveformBinaryFileWriter waveformBinaryFileWriter = new WaveformBinaryFileWriter(waveformBinaryResultPath)) {
            extractWaveformInformationFrom16BitLittleEndian(audioFile, waveformBinaryFileWriter);
        } catch (Exception e) {
            throw new AudioEngineException("Error during creating waveform binary for audio file", e);
        }
    }

    public void createFrequencyBinary(PcmMono16BitLittleEndian audioFile, Path frequencyBinaryResultPath) throws AudioEngineException {
        logger.log(Level.FINE, "Extracting frequency information from file {0}", audioFile.getAudioFilePath());

        try (FrequencyFileWriter frequencyFileWriter = new FrequencyFileWriter(frequencyBinaryResultPath, FREQUENCY_EXTRACTION_INTERVAL_MS)) {
            extractFrequencyInformationFrom16BitLittleEndian(audioFile, frequencyFileWriter);
        } catch (Exception e) {
            throw new AudioEngineException("Error during extracting frequency information from file", e);
        }
    }

    private static WaveformDataPoint createWaveformDataPoint(DataInputStream dataInputStream, long numberOfSamples) throws IOException {
        long numberOfReadSamples = 0;
        float currentNormalizedMinValue = Float.MAX_VALUE;
        float currentNormalizedMaxValue = Float.MIN_VALUE;
        float summedNormalizedValues = 0;

        while (numberOfReadSamples < numberOfSamples && dataInputStream.available() >= Short.BYTES) {
            short bigEndianCurrentSample = dataInputStream.readShort();
            short littleEndianCurrentSample = Short.reverseBytes(bigEndianCurrentSample);

            float normalizedValue = Math.abs((float) littleEndianCurrentSample / 32768.0f);
            if (currentNormalizedMinValue > normalizedValue) {
                currentNormalizedMinValue = normalizedValue;
            }

            if (currentNormalizedMaxValue < normalizedValue) {
                currentNormalizedMaxValue = normalizedValue;
            }

            summedNormalizedValues += normalizedValue;
            numberOfReadSamples++;
        }

        float averageNormalizedValues = summedNormalizedValues / numberOfReadSamples;
        return new WaveformDataPoint(currentNormalizedMinValue, currentNormalizedMaxValue, averageNormalizedValues);
    }

    private static void extractWaveformInformationFrom16BitLittleEndian(PcmMono16BitLittleEndian pcmFile, WaveformBinaryFileWriter waveformBinaryFileWriter) throws AudioEngineException {
        File monoFile = pcmFile.getAudioFilePath().toFile();
        long fileLength = monoFile.length();
        long sampleCount = fileLength / 2; // 2 Bytes per sample

        long numberOfSamplesPerWaveformDataPoint;
        long waveformSegmentCount;
        if (sampleCount >= WAVEFORM_SEGMENT_COUNT) {
            waveformSegmentCount = WAVEFORM_SEGMENT_COUNT;
            numberOfSamplesPerWaveformDataPoint = Math.round((float) sampleCount / WAVEFORM_SEGMENT_COUNT);
        } else {
            numberOfSamplesPerWaveformDataPoint = 1;
            waveformSegmentCount = sampleCount;
        }

        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(monoFile))) {
            for (int i = 0; i < waveformSegmentCount; i++) {
                WaveformDataPoint waveformDataPoint = FfmpegAudioEngine.createWaveformDataPoint(dataInputStream, numberOfSamplesPerWaveformDataPoint);
                waveformBinaryFileWriter.writeValues(waveformDataPoint);
            }
        } catch (Exception e) {
            throw new AudioEngineException("Error during extracting Waveform information from mono file", e);
        }
    }

    private static List<Double> readNextNormalizedSampleValuesFrom16BitLittleEndian(DataInputStream dataInputStream, int numberOfSamples) throws IOException {
        List<Double> result = new ArrayList<>(numberOfSamples);

        for (int i = 0; i < numberOfSamples; i++) {
            if (dataInputStream.available() >= Short.BYTES) {
                short bigEndianCurrentSample = dataInputStream.readShort();
                short littleEndianCurrentSample = Short.reverseBytes(bigEndianCurrentSample);
                double normalizedValue = littleEndianCurrentSample / 32768.0;

                result.add(normalizedValue);
            } else {
                break;
            }
        }

        return result;
    }

    private static void extractFrequencyInformationFrom16BitLittleEndian(PcmMono16BitLittleEndian pcmFile, FrequencyFileWriter frequencyFileWriter) throws AudioEngineException {
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(pcmFile.getAudioFilePath().toFile()))) {
            while (dataInputStream.available() > 0) {
                List<Double> currentSamples = readNextNormalizedSampleValuesFrom16BitLittleEndian(dataInputStream, SAMPLES_PER_FREQUENCY_DATA_POINT);
                double dominantFrequency = findDominantFrequency(currentSamples.stream().mapToDouble(Double::doubleValue).toArray());
                frequencyFileWriter.writeFrequencyDataPoint(dominantFrequency);
            }
        } catch (Exception e) {
            throw new AudioEngineException("Error during extracting frequency information from mono file", e);
        }
    }

    private static double findDominantFrequency(double[] samples) throws AudioEngineException {
        int fftSize = nextPowerOfTwo(samples.length);

        double[] paddedSamples = new double[fftSize];
        System.arraycopy(samples, 0, paddedSamples, 0, samples.length);

        applyHammingWindow(paddedSamples, samples.length);

        Complex[] fftResult = fft(paddedSamples);

        double[] magnitudes = new double[fftResult.length / 2];
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = fftResult[i].magnitude();
        }

        int maxIndex = 1;
        for (int i = 2; i < magnitudes.length; i++) {
            if (magnitudes[i] > magnitudes[maxIndex]) {
                maxIndex = i;
            }
        }

        return (double) maxIndex * SAMPLE_RATE / fftSize;
    }

    private static void applyHammingWindow(double[] samples, int actualLength) {
        for (int i = 0; i < actualLength; i++) {
            double window = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (actualLength - 1));
            samples[i] *= window;
        }
    }

    private static int nextPowerOfTwo(int n) {
        return Integer.highestOneBit(n - 1) << 1;
    }

    private static Complex[] fft(double[] samples) throws AudioEngineException {
        int n = samples.length;
        Complex[] x = new Complex[n];

        // Input in Complex Array konvertieren
        for (int i = 0; i < n; i++) {
            x[i] = new Complex(samples[i], 0);
        }

        return fft(x);
    }

    private static Complex[] fft(Complex[] x) throws AudioEngineException {
        int n = x.length;

        if (n == 1) return x;
        if (n % 2 != 0) throw new AudioEngineException("n has to be multiple of 2");

        Complex[] even = new Complex[n / 2];
        Complex[] odd = new Complex[n / 2];
        for (int i = 0; i < n / 2; i++) {
            even[i] = x[2 * i];
            odd[i] = x[2 * i + 1];
        }

        Complex[] evenFFT = fft(even);
        Complex[] oddFFT = fft(odd);

        Complex[] result = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double angle = -2 * Math.PI * k / n;
            Complex wk = new Complex(Math.cos(angle), Math.sin(angle));
            Complex t = wk.multiply(oddFFT[k]);
            result[k] = evenFFT[k].add(t);
            result[k + n / 2] = evenFFT[k].subtract(t);
        }

        return result;
    }
}
