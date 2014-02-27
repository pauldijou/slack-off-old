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

object Commands extends Controller {
  val token = "FGbEeBew4N8NiCwcBcK9Qp3e"

  def error(msg: String) = Ok("ERROR: " + msg)
  def asyncError(msg: String) = Future(Ok("ERROR: " + msg))

  val commandForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "team_id" -> nonEmptyText,
      "channel_id" -> nonEmptyText,
      "channel_name" -> nonEmptyText,
      "user_id" -> nonEmptyText,
      "user_name" -> nonEmptyText,
      "command" -> nonEmptyText,
      "text" -> nonEmptyText
    )(SlackCommand.apply)(SlackCommand.unapply)
  )

  def redirectToCommand(command: SlackCommand): Future[SimpleResult] = command.command match {
    case "/do" => {
      command.args.headOption match {
        case None => asyncError("/do command must have at least one argument.")
        case Some(newCommand) => {
          val newText = command.args.drop(1).mkString(" ")
          redirectToCommand(command.copy(command = "/" + newCommand, text = newText))
        }
      }
    }
    case "/jira" => handleJira(command)
    case _ => asyncError("unknow command '" + command.command + "'.")
  }

  def handle = Action.async { implicit request =>
    commandForm.bindFromRequest.fold(
      errors => asyncError("wrong command submission from Slack."),
      command => {
        if (command.token != token) asyncError("wrong token.")
        else redirectToCommand(command)
      }
    )
  }

  def handleJira(command: SlackCommand): Future[SimpleResult] = {
    command.args.headOption match {
      case Some("") => asyncError("/jira command must have at least one argument.")
      case Some("new") => {
        JiraApi.create(command.args(2), command.args(3), command.channel_name.toUpperCase, command.args(1)).map { r =>
          if (r.keys.contains("key")) {
            val key = (r \ "key").toString
            println("Added jira " + key)
            val link = JiraApi.issueUrl(key)
            Ok(s"<@${command.user_name}> juste created JIRA <${link}|${key}>")
          } else {
            error(r.toString)
          }
        }
      }
      case Some(key) => {
        val link = JiraApi.issueUrl(key)
        Future(Ok(s"<${link}|${key}>"))
      }
      case _ => asyncError("/jira command must have at least one argument.")
    }
  }
}
