package services

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS

import models.IncomingWebHook

object IncomingWebhooks extends utils.Config {
  lazy val url = s"https://${slackTeam}.slack.com/services/hooks/incoming-webhook?token=${slackTokenIncoming}"

  def send(hook: IncomingWebHook) = {
    val jsWebhook = Json.toJson(hook)
    val jsonWebhook = Json.stringify(jsWebhook);
    println("-------------------------")
    println(jsonWebhook)
    println("-------------------------")

    WS.url(url).post(jsonWebhook)
  }
}