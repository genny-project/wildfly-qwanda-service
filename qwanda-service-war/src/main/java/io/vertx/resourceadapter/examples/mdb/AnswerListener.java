package io.vertx.resourceadapter.examples.mdb;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.annotations.Merge;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.Answer;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;

@ApplicationScoped
public class AnswerListener {


	@Incoming("answer")
	@Merge
	public CompletionStage<Void> fromAnswers(Message<String>  message) {
		JsonObject json = new JsonObject(message.getPayload());
		String token = json.getString("token");
		json.remove("token");
		Answer answer = JsonUtils.fromJson(json.toString(),Answer.class);

		try {
			QwandaUtils.apiPostEntity2("http://localhost:8080/qwanda/answers",
					JsonUtils.toJson(answer), token, null);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return	message.ack();
	}

}
