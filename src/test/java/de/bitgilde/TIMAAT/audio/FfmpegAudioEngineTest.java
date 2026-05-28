package de.bitgilde.TIMAAT.audio;

import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.PropertyManagement;
import de.bitgilde.TIMAAT.processing.audio.FfmpegAudioEngine;
import de.bitgilde.TIMAAT.processing.audio.api.FrequencyInformation;
import de.bitgilde.TIMAAT.processing.audio.api.PcmMono16BitLittleEndian;
import de.bitgilde.TIMAAT.processing.audio.exception.AudioEngineException;
import de.bitgilde.TIMAAT.processing.audio.io.FrequencyFileReader;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testsuite of {@link FfmpegAudioEngine}
 *
 * @author Nico Kotlenga
 * @since 11.08.25
 */
public class FfmpegAudioEngineTest {

    private static final Path TEST_OUTPUT_DIRECTORY = Paths.get("./test_output");

    @BeforeAll
    public static void setup() throws IOException {
        Files.createDirectories(TEST_OUTPUT_DIRECTORY);
    }

    /**
     * Tests the extraction of frequency information.
     * The test file has 2 second intervals of following frequencies:
     * <ul>
     *     <li>50Hz</li>
     *     <li>100Hz</li>
     *     <li>150Hz</li>
     * </ul>
     *
     * @throws InstantiationException
     */
    @Test
    public void shouldReadFrequencyInformationCorrectly() throws InstantiationException, AudioEngineException, IOException {
        PropertyManagement propertyManagement = mock(PropertyManagement.class);
        when(propertyManagement.getProp(eq(PropertyConstants.STORAGE_TEMP_LOCATION))).thenReturn(TEST_OUTPUT_DIRECTORY.toString());
        when(propertyManagement.getProp(eq(PropertyConstants.FFMPEG_LOCATION))).thenReturn("ffmpeg");

        TemporaryFileStorage temporaryFileStorage = new TemporaryFileStorage(propertyManagement);
        FfmpegAudioEngine ffmpegAudioEngine = new FfmpegAudioEngine(propertyManagement, temporaryFileStorage);

        Path audioPath = Path.of(FfmpegAudioEngineTest.class.getResource("/test-audio/test_frequency.pcm").getPath());
        PcmMono16BitLittleEndian pcmMono16BitLittleEndian = mock(PcmMono16BitLittleEndian.class);
        when(pcmMono16BitLittleEndian.getAudioFilePath()).thenReturn(audioPath);

        Path frequencyOutputPath = TEST_OUTPUT_DIRECTORY.resolve("test_frequency.frequency");

        ffmpegAudioEngine.createFrequencyBinary(pcmMono16BitLittleEndian, frequencyOutputPath);
        Assertions.assertTrue(Files.exists(frequencyOutputPath));

        FrequencyFileReader frequencyFileReader = new FrequencyFileReader(frequencyOutputPath);
        Optional<FrequencyInformation> firstInterval = frequencyFileReader.getFrequencyInformation(0, 2000);
        Assertions.assertTrue(firstInterval.isPresent());
        Assertions.assertEquals(50.0, firstInterval.get().getMinimumFrequency(), 1);
        Assertions.assertEquals(50.0, firstInterval.get().getMaximumFrequency(), 1);

        Optional<FrequencyInformation> secondInterval = frequencyFileReader.getFrequencyInformation(2000, 4000);
        Assertions.assertTrue(secondInterval.isPresent());
        Assertions.assertEquals(100.0, secondInterval.get().getMinimumFrequency(), 1);
        Assertions.assertEquals(100.0, secondInterval.get().getMaximumFrequency(), 1);

        Optional<FrequencyInformation> thirdInterval = frequencyFileReader.getFrequencyInformation(4000, 6000);
        Assertions.assertTrue(thirdInterval.isPresent());
        Assertions.assertEquals(150, thirdInterval.get().getMinimumFrequency(), 1);
        Assertions.assertEquals(150, thirdInterval.get().getMaximumFrequency(), 1);

        Optional<FrequencyInformation> wholeFile = frequencyFileReader.getFrequencyInformation(null, null);
        Assertions.assertTrue(wholeFile.isPresent());
        Assertions.assertEquals(50, wholeFile.get().getMinimumFrequency(), 1);
        Assertions.assertEquals(150, wholeFile.get().getMaximumFrequency(), 1);

        Optional<FrequencyInformation> firstAndSecondInterval = frequencyFileReader.getFrequencyInformation(0, 4000);
        Assertions.assertTrue(firstAndSecondInterval.isPresent());
        Assertions.assertEquals(50, firstAndSecondInterval.get().getMinimumFrequency(), 1);
        Assertions.assertEquals(100, firstAndSecondInterval.get().getMaximumFrequency(), 1);


        Optional<FrequencyInformation> secondAndThirdInterval = frequencyFileReader.getFrequencyInformation(2000, 6000);
        Assertions.assertTrue(secondAndThirdInterval.isPresent());
        Assertions.assertEquals(100, secondAndThirdInterval.get().getMinimumFrequency(), 1);
        Assertions.assertEquals(150, secondAndThirdInterval.get().getMaximumFrequency(), 1);
    }

    /**
     * Verifies that the engine writes the converted mono PCM data as a raw stream (no container)
     * when {@code persistInContainerFormat} is {@code false}.
     *
     * @throws Exception if test setup or ffmpeg invocation fails
     */
    @Test
    public void shouldPersistOutputAsRawPcmWhenNotUsingContainerFormat() throws Exception {
        Path ffmpegDirectory = findFfmpegDirectory();
        Assumptions.assumeTrue(ffmpegDirectory != null, "ffmpeg not available on PATH");

        PropertyManagement propertyManagement = mock(PropertyManagement.class);
        when(propertyManagement.getProp(eq(PropertyConstants.STORAGE_TEMP_LOCATION))).thenReturn(TEST_OUTPUT_DIRECTORY.toString());
        when(propertyManagement.getProp(eq(PropertyConstants.FFMPEG_LOCATION))).thenReturn(ffmpegDirectory.toString());

        TemporaryFileStorage temporaryFileStorage = new TemporaryFileStorage(propertyManagement);
        FfmpegAudioEngine ffmpegAudioEngine = new FfmpegAudioEngine(propertyManagement, temporaryFileStorage);

        Path inputPath = TEST_OUTPUT_DIRECTORY.resolve("convert_input_raw.wav");
        writeSilentWav(inputPath);

        try (PcmMono16BitLittleEndian result = ffmpegAudioEngine.convertAudioChannelsTo16BitLittleEndian(inputPath, false)) {
            byte[] header = readFirstBytes(result.getAudioFilePath(), 4);
            Assertions.assertNotEquals("RIFF", new String(header, StandardCharsets.US_ASCII),
                    "Expected raw PCM output without RIFF container header");
        }
    }

    /**
     * Verifies that the engine wraps the converted mono PCM data inside a WAV container
     * when {@code persistInContainerFormat} is {@code true}.
     *
     * @throws Exception if test setup or ffmpeg invocation fails
     */
    @Test
    public void shouldPersistOutputInsideWavContainerWhenUsingContainerFormat() throws Exception {
        Path ffmpegDirectory = findFfmpegDirectory();
        Assumptions.assumeTrue(ffmpegDirectory != null, "ffmpeg not available on PATH");

        PropertyManagement propertyManagement = mock(PropertyManagement.class);
        when(propertyManagement.getProp(eq(PropertyConstants.STORAGE_TEMP_LOCATION))).thenReturn(TEST_OUTPUT_DIRECTORY.toString());
        when(propertyManagement.getProp(eq(PropertyConstants.FFMPEG_LOCATION))).thenReturn(ffmpegDirectory.toString());

        TemporaryFileStorage temporaryFileStorage = new TemporaryFileStorage(propertyManagement);
        FfmpegAudioEngine ffmpegAudioEngine = new FfmpegAudioEngine(propertyManagement, temporaryFileStorage);

        Path inputPath = TEST_OUTPUT_DIRECTORY.resolve("convert_input_wav.wav");
        writeSilentWav(inputPath);

        try (PcmMono16BitLittleEndian result = ffmpegAudioEngine.convertAudioChannelsTo16BitLittleEndian(inputPath, true)) {
            byte[] header = readFirstBytes(result.getAudioFilePath(), 12);
            Assertions.assertEquals("RIFF", new String(header, 0, 4, StandardCharsets.US_ASCII));
            Assertions.assertEquals("WAVE", new String(header, 8, 4, StandardCharsets.US_ASCII));
        }
    }

    private static Path findFfmpegDirectory() {
        String pathEnvironmentVariable = System.getenv("PATH");
        if (pathEnvironmentVariable == null) {
            return null;
        }
        for (String pathEntry : pathEnvironmentVariable.split(File.pathSeparator)) {
            Path candidate = Paths.get(pathEntry, "ffmpeg");
            if (Files.isExecutable(candidate)) {
                return candidate.getParent();
            }
        }
        return null;
    }

    private static void writeSilentWav(Path outputPath) throws IOException {
        AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
        int sampleCount = 4410;
        byte[] silentSamples = new byte[sampleCount * 2];
        try (AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(silentSamples), format, sampleCount)) {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputPath.toFile());
        }
    }

    private static byte[] readFirstBytes(Path path, int byteCount) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return inputStream.readNBytes(byteCount);
        }
    }
}
