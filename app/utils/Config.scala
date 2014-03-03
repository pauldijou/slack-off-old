package utils

import play.api.Play

trait Config {
  def config = Config.config
  def getString(key: String) = Config.getString(key)

  // JIRA
  object jira {
    def url = getString("jira.url")
    def authBasic = getString("jira.auth.basic")

    object bot {
      def name = config.getString("jira.bot.name")
      def icon = config.getString("jira.bot.icon")
    }

    object colors {
      object issue {
        def created = config.getString("jira.colors.issue.created")
        def updated = config.getString("jira.colors.issue.updated")
        def deleted = config.getString("jira.colors.issue.deleted")
      }

      def comment =  config.getString("jira.colors.comment")
    }
  }

  // BITBUCKET
  object bitbucket {
    object bot {
      def name = config.getString("bitbucket.bot.name")
      def icon = config.getString("bitbucket.bot.icon")
    }
  }

  // MESSAGES
  object messages {
    object jira {
      def enabled = config.getBoolean("messages.jira.enabled") getOrElse false
      def regex = getString("messages.jira.regex")
    }
  }

  // SLACK
  object slack {
    object team {
      def name = getString("slack.team.name")
    }

    object hooks {
      object incoming {
        def token = getString("slack.hooks.incoming.token")
      }
    }
  }
}

object Config {
  lazy val config = Play.current.configuration
  def getString(key: String): String = config.getString(key) getOrElse ""
}
