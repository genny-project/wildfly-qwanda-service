package io.vertx.resourceadapter.examples.mdb;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class Producer {


  @Inject @Channel("eventsout") Emitter<String> events;
  public Emitter<String> getToEvents() {
    return events;
  }

  @Inject @Channel("dataout") Emitter<String> data;
  public Emitter<String> getToData() {
    return data;
  }

  @Inject @Channel("cmdsout") Emitter<String> cmds;
  public Emitter<String> getToCmds() {
    return cmds;
  }

  @Inject @Channel("messagesout") Emitter<String> messages;
  public Emitter<String> getToMessages() {
    return messages;
  }

  @Inject @Channel("webdataout") Emitter<String> webData;
  public Emitter<String> getToWebData() {
    return webData;
  }

  @Inject @Channel("webcmdsout") Emitter<String> webCmds;
  public Emitter<String> getToWebCmds() {
    return webCmds;
  }

  @Inject @Channel("socialout") Emitter<String> social;
  public Emitter<String> getToSocial() {
    return social;
  }

  @Inject @Channel("signalsout") Emitter<String> signals;
  public Emitter<String> getToSignals() {
    return signals;
  }

  @Inject @Channel("healthout") Emitter<String> health;
  public Emitter<String> getToHealth() {
    return health;
  }

  @Inject @Channel("statefulmessagesout") Emitter<String> statefulmessages;
  public Emitter<String> getToStatefulMessages() {
    return statefulmessages;
  }

  @Inject @Channel("servicesout") Emitter<String> services;
  public Emitter<String> getToServices() {
    return services;
  }

}

