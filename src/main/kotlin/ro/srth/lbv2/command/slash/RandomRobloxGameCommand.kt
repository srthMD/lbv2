package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import okhttp3.Request
import org.json.JSONObject
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand

@Suppress("unused")
class RandomRobloxGameCommand(data: Data) : LBCommand(data) {

    private companion object {
        const val APIPREFIX = "https://games.roblox.com/v2/users/"
        const val APISUFFIX = "/games?accessFilter=2&limit=50"
        const val SIMPLEPREFIX = "https://www.roblox.com/games/"
    }

    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val user = event.getOption("userid", OptionMapping::getAsString)

        if (user == null) {
            getRandomGame(event)
        } else {
            getRandomUserGame(user, event)
        }
    }

    private fun getRandomUserGame(user: String, event: SlashCommandInteractionEvent) {
        val request = Request.Builder()
            .url(APIPREFIX + user + APISUFFIX)
            .build()

        val call = Bot.getClient().newCall(request)
        val response = call.execute()

        if (response.isSuccessful) {
            val data = JSONObject(response.body!!.string())

            val arr = data.getJSONArray("data")

            val index = Bot.rand().nextInt(0, arr.length())

            val game = arr.getJSONObject(index).getJSONObject("rootPlace").getLong("id")

            event.reply(SIMPLEPREFIX + game).queue()
        } else {
            event.reply("Unable to get games, user may not exist.").queue()
        }
    }

    private fun getRandomGame(event: SlashCommandInteractionEvent) {
        var link = "$SIMPLEPREFIX{Bot.rand().nextLong(0, 1000000000)}"

        for (i in 0..10) {
            val request = Request.Builder()
                .url(link)
                .build()

            val call = Bot.getClient().newCall(request)
            val response = call.execute()

            when {
                response.code != 404 -> {
                    event.reply(link).queue()
                    return
                }

                else -> link = "https://www.roblox.com/games/${Bot.rand().nextLong(0, 1000000000)}"
            }
        }

        event.reply("Failed to find a valid game after 10 requests, please try again.").queue()
    }
}