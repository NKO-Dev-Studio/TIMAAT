package de.bitgilde.TIMAAT.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.bitgilde.TIMAAT.security.TIMAATKeyGenerator;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@ServerEndpoint("/api/notification")
public class NotificationWebSocket {

  private static Map<String, UserSubscriptions> userSessions = new HashMap<String, UserSubscriptions>();

  @OnOpen
  public void onOpen(Session session) {
    System.out.println("TIMAAT::Notification:new session " + session.getId() + " - " + session.getMaxIdleTimeout());
  }

  @OnClose
  public void onClose(Session session) {
    System.out.println("TIMAAT::Notification:end session " + session.getId());
    endUserSession(session);
  }

  @OnMessage
  public void onMessage(String message, Session session) {
    // System.out.println("onMessage::From=" + session.getId() + " Message=" + message);

    try {
      NotificationRequest req = new ObjectMapper().readValue(message, NotificationRequest.class);
      String username = validateRequestToken(req.getToken());

      if (req.getRequest() == null || !req.getRequest().equalsIgnoreCase("subscribe-list")) {
        return;
      }

      if (username != null) {
        // TODO check if user account status has expired or is blocked
        if (!userSessions.containsKey(session.getId())) {
          createUserSession(username, session);
        }
        notifyUserSubscription(username, req.dataID, session);
        // answer user subscription request
        if (req.getDataID() > 0) {
          acknowledgeSubscriptionInfo(username, session, req.dataID);
        }
      }
    } catch (Exception e) {
      System.out.println("TIMAAT::Notification:client message failed to decode");
    }
  }

  private void acknowledgeSubscriptionInfo(String subscriber, Session session, int listID) {
    List<String> subscribers = new ArrayList<String>();
    for (UserSubscriptions userSub : userSessions.values()) {
      if (!userSub.username.equalsIgnoreCase(subscriber) && userSub.getAnalysisList() == listID) {
        subscribers.add(userSub.getUsername());
      }
    }
    if (subscribers.size() > 0) {
      sendNotificationTo(session, subscriber, "list-subscribers", listID, subscribers);
    }
  }

  private void notifyUserSubscription(String username, int listID, Session session) {
    UserSubscriptions userSub = userSessions.get(session.getId());
    if (userSub == null) {
      return;
    }
    int oldListID = userSub.getAnalysisList();
    if (oldListID > 0) {
      notifyUserUnsubscribe(username, oldListID);
    }
    userSub.setAnalysisList(listID);
    if (listID > 0) {
      for (UserSubscriptions sub : userSessions.values()) {
        if (!sub.getUsername().equalsIgnoreCase(username) && sub.getAnalysisList() == listID) {
          sendNotificationTo(sub.session, username, "subscribe-list", listID);
        }
      }
    }
  }

  public static void notifyUserAction(String username, String action, int listID, Object data) {
    if (username == null || action == null) {
      return;
    }
    for (UserSubscriptions userSub : userSessions.values()) {
      if (!userSub.getUsername().equalsIgnoreCase(username) && userSub.getAnalysisList() == listID) {
        sendNotificationTo(userSub.session, username, action, listID, data);
      }
    }
  }

  private void notifyUserUnsubscribe(String username, int listID) {
    if (listID <= 0) {
      return;
    }
    for (UserSubscriptions userSub : userSessions.values()) {
      if (!userSub.getUsername().equalsIgnoreCase(username) && userSub.getAnalysisList() == listID) {
        sendNotificationTo(userSub.session, username, "unsubscribe-list", listID);
      }
    }
  }

  private static void sendNotificationTo(Session session, String username, String message, int dataID) {
    sendNotificationTo(session, username, message, dataID, null);
  }

  private static void sendNotificationTo(Session session, String username, String message, int dataID, Object data) {
    try {
      NotificationMessage notification = new NotificationMessage(username, message, dataID);
      if (data != null) {
        notification.setData(data);
      }
      ObjectWriter ow = new ObjectMapper().writer();
      session.getAsyncRemote().sendText(ow.writeValueAsString(notification));
    } catch (Exception e) {
      // TODO remove session
    }
  }

  private void endUserSession(Session session) {
    UserSubscriptions userSub = userSessions.get(session.getId());
    if (userSub == null) {
      return;
    }
    if (userSub.getAnalysisList() > 0) {
      notifyUserUnsubscribe(userSub.getUsername(), userSub.getAnalysisList());
    }
    try {
      if (session.isOpen()) {
        session.close();
      }
      userSessions.remove(session.getId());
    } catch (Exception e) {
      userSessions.remove(session.getId());
    }
  }


  private void createUserSession(String username, Session session) {
    userSessions.put(session.getId(), new UserSubscriptions(username, session));
  }

  @OnError
  public void onError(Throwable t) {
    // ignore
  }

  private String validateRequestToken(String token) {
    String username = null;
    try {
      SecretKey key = TIMAATKeyGenerator.generateKey();
      username = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    } catch (JwtException e) {
      e.printStackTrace();
      return null;
    }

    return username;

  }
}
