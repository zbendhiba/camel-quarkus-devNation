package com.example;

import com.example.model.TelegramMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.telegram.TelegramConstants;
import org.apache.camel.component.telegram.TelegramParseMode;

public class Routes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("telegram:bots?authorizationToken={{telegram-token-api}}")
                .log("Incoming message ${body}")
                .bean(TelegramBean.class)
                .to("direct:process-msg");

        from("direct:process-msg")
                .choice()
                    .when(simple("${body} contains '/start'"))
                        .transform(simple("{{devNation.bot.start}}"))
                    .when(simple("${body} contains '#msg'"))
                        .to("kafka:topic-bot")
                        .transform(simple("{{devNation.bot.msg}}"))
                    .otherwise()
                        .transform(simple("{{devNation.bot.otherwise}}"))
                .end()
                .setHeader(TelegramConstants.TELEGRAM_PARSE_MODE, simple(TelegramParseMode.MARKDOWN.name()))
                .to("telegram:bots?authorizationToken={{telegram-token-api}}");

        from("kafka:topic-bot")
                .log("Incoming message from kafka : ${body}")
                .unmarshal().json(TelegramMessage.class)
                .to("jpa:" + TelegramMessage.class.getName());

        from("platform-http:/messages?httpMethodRestrict=GET")
                .to("jpa:" + TelegramMessage.class.getName() + "?namedQuery=findAll")
                .marshal().json();
    }
}
