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

object Messages extends Controller with utils.Config with utils.Log {
  lazy val logger = Logger("hooks.messages")

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
      "service_id" -> optional(text),
      "trigger_word" -> optional(text)
    )(OutgoingWebHook.apply)(OutgoingWebHook.unapply)
  )

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
          var handlersBuffer = scala.collection.mutable.ListBuffer[Future[(String, String)]]()

          if (messages.jira.enabled) { handlersBuffer += handleJira(hook) }

          Future.sequence(handlersBuffer.result).map { responses =>
            responses.foreach { response =>
              if (!response._2.isEmpty) {
                appendUsername(response._1)
                appendResponse(response._2)
              }
            }
          }.map { _ =>
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

  def handleJira(hook: OutgoingWebHook): Future[(String, String)] = {
    Future.sequence((for {
      jiraRegex(issueKey) <- jiraRegex findAllIn hook.content
    } yield issueKey).toList map {
      key => JiraApi.get(key).map { (key, _) }
    }).map { issues =>
      ("JIRA",
      issues.map {
        case (key, None) => s"${key}: No issue found, sorry."
        case (key, Some(issue)) => {
          val link = JiraApi.issueUrl(issue.key)
          s"<${link}|${issue.key}> [${issue.fields.priority.name}]: ${issue.fields.summary} (by ${issue.fields.creator.displayName})"
        }
      }.mkString("\n"))
    }
  }
}
