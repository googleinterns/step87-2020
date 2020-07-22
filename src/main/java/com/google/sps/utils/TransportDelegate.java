package com.google.sps.utils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

public class TransportDelegate {
  public void send(Message msg) throws MessagingException {
    Transport.send(msg);
  }
}
