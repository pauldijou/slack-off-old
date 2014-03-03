package services

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS

import models.IncomingWebHook

object IncomingWebhooks extends utils.Config with utils.Log {
  lazy val logger = play.api.Logger("hooks.incoming")
  lazy val url =
    s"https://${slack.team.name}.slack.com/services/hooks/incoming-webhook?token=${slack.hooks.incoming.token}"

  def send(hook: IncomingWebHook) = {
    val jsWebhook = Json.toJson(hook)

    debugStart("IncomingWebhooks.send")
    debug(Json.prettyPrint(jsWebhook))
    debugEnd

    WS.url(url).post(Json.stringify(jsWebhook))
  }
}
