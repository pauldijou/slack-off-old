package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import play.api._
import play.api.mvc._
import play.api.libs.ws.WS
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._

import models._
import models.JiraWebhookAction._

import services._
import models._

object OutgoingWebHooks extends Controller with utils.Log {
  lazy val logger = Logger("hooks.outgoing")

  def asyncError(msg: String) = Future(Ok("ERROR: " + msg))

  val jiraRegex = "([a-zA-Z]+-[0-9]+)".r

  val hookForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "team_id" -> nonEmptyText,
      "team_domain" -> nonEmptyText,
      "channel_id" -> nonEmptyText,
      "channel_name" -> nonEmptyText,
      "timestamp" -> nonEmptyText,
      "user_id" -> nonEmptyText,
      "user_name" -> nonEmptyText,
      "text" -> optional(text),
      "service_id" -> optional(text)
    )(OutgoingWebHook.apply)(OutgoingWebHook.unapply)
  )

  def get = Action {
    Ok("You get it")
  }

  def handle = Action.async { implicit request =>
    var username = ""
    var response = ""
    var hasJira = false

    def appendUsername(name: String) = {
      if (username.isEmpty) { username = name }
      else { username = username + " | " + name }
    }

    def appendResponse(message: String) = {
      if (response.isEmpty) { response = message }
      else { response = response + "\n" + message }
    }

    debugStart("OutgoingWebHooks.handle");
    hookForm.bindFromRequest.fold(
      errors => {
        debug("ERROR parsing: " + request.body)
        Future(IncomingWebHook("Something went really wrong", Some("ERROR")).toResult)
      },
      hook => {
        debug(hook.toString)
        if (!hook.acceptable) { Future(Ok) }
        else {
          // Handle JIRA expressions
          Future.sequence((for {
            jiraRegex(issueKey) <- jiraRegex findAllIn hook.content
          } yield issueKey).toList map {
            key => JiraApi.get(key).map { (key, _) }
          }).map { issues =>
            issues foreach {
              case (key, None) => appendResponse(s"${key}: No issue found, sorry.")
              case (key, Some(issue)) => {
                hasJira = true
                val link = JiraApi.issueUrl(issue.key)
                appendResponse(s"<${link}|${issue.key}> [${issue.fields.priority.name}]: ${issue.fields.summary} (by ${issue.fields.creator.displayName})")
              }
            }

            if (hasJira) appendUsername("JIRA")
            if (username.isEmpty) appendUsername("ZenBot")

            Ok(Json.stringify(Json.obj(
              "username" -> username,
              "text" -> response
            )))
          }
        }
      }
    )
  }
}
