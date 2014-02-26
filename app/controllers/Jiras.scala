package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._
import models.JiraWebhookAction._

import services._

object Jiras extends Controller {
  
  def handleWebhook = Action(parse.json) { implicit request =>
    request.body.validate[JiraWebhookEvent].fold(
      errors => println(errors),
      event => {
        val action = event.webhookEvent
        val username = request.getQueryString("username") orElse Some("JIRA")
        val channel = request.getQueryString("channel").map("#" + _)
        val iconUrl = request.getQueryString("iconUrl") orElse Some("https://slack.global.ssl.fastly.net/14542/img/services/jira_32.png")

        val issue = event.issue
        val fields = issue.fields

        val issueName = event.issue.key
        val issueLink = "https://zenstudio.atlassian.net/browse/" + issueName
        val issueType =fields.issuetype.name
        val updatedBy = event.user.displayName
        println(issueName + " | " + issueLink)

        val message = action match {
          case ISSUE_CREATED => s"${issueType} <${issueLink}|${issueName}> has been created by ${updatedBy}."
          case ISSUE_UPDATED => s"${issueType} <${issueLink}|${issueName}> has been updated by ${updatedBy}."
          case ISSUE_DELETED => s"${issueType} <${issueLink}|${issueName}> has been deleted by ${updatedBy}."
          case WORKLOG_UPDATED => s"Worklog of <${issueLink}|${issueName}> has been updated by ${updatedBy}."
        }

        val attachmentIssueSummary = IncomingWebHookAttachmentField("Summary", fields.description)
        val attachmentIssueCreator = IncomingWebHookAttachmentField("Creator", fields.creator.displayName)

        val webhook = IncomingWebHook(
          message,
          username,
          channel,
          iconUrl,
          None,
          Some(List(
            IncomingWebHookAttachment(
              "Fallback",
              None,
              None,
              None,
              List(attachmentIssueSummary, attachmentIssueCreator)
            )
          ))
        )

        IncomingWebhooks.send(webhook)
      }
    )

    Ok
  }
  
}