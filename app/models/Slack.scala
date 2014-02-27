package models

import play.api._
import play.api.mvc._
import play.api.mvc.Results.Ok
import play.api.libs.json._
import play.api.libs.functional.syntax._

// IncomingWebHook -----------------

case class IncomingWebHookAttachmentField(title: String, value: String, short: Boolean = false)

object IncomingWebHookAttachmentField {
  implicit val incomingWebHookAttachmentFieldFormat = Json.format[IncomingWebHookAttachmentField]
}

case class IncomingWebHookAttachment(
  fallback: String,
  text: Option[String] = None,
  pretext: Option[String] = None,
  color: Option[String] = None, // Hex code or 'good' or 'warning' or 'danger'
  fields: List[IncomingWebHookAttachmentField] = List()
)

object IncomingWebHookAttachment {
  implicit val incomingWebHookAttachmentFormat = Json.format[IncomingWebHookAttachment]
}

case class IncomingWebHook(
  text: String,
  username: Option[String] = None,
  channel: Option[String] = None,
  icon_url: Option[String] = None,
  icon_emoji: Option[String] = None,
  attachments: Option[List[IncomingWebHookAttachment]] = None
) {
  def toResult: SimpleResult = Ok(Json.stringify(Json.toJson(this)(IncomingWebHook.incomingWebHookFormat)))
}

object IncomingWebHook {
  implicit val incomingWebHookFormat = Json.format[IncomingWebHook]
}

// OutgoingWebHook -----------------

case class OutgoingWebHook(
  token: String,
  team_id: String,
  channel_id: String,
  channel_name: String,
  timestamp: String,
  user_id: String,
  user_name: String,
  text: String
)

// SlackCommand -----------------

case class SlackCommand(
  token: String,
  team_id: String,
  channel_id: String,
  channel_name: String,
  user_id: String,
  user_name: String,
  command: String,
  text: String
) {
  lazy val args: List[String] = text.split(" ").toList
}
