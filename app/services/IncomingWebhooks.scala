package services

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS

import models.IncomingWebHook

object IncomingWebhooks extends utils.Config with utils.Log {
  lazy val logger = play.api.Logger("hooks.incoming")
  lazy val url = s"https://${slackTeam}.slack.com/services/hooks/incoming-webhook?token=${slackTokenIncoming}"

  def send(hook: IncomingWebHook) = {
    val jsWebhook = Json.toJson(hook)
    val jsonWebhook = Json.stringify(jsWebhook)

    debugStart("IncomingWebhooks.send")
    debug(Json.prettyPrint(jsWebhook))
    debugEnd

    WS.url(url).post(jsonWebhook)
  }
}
