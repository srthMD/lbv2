package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import okhttp3.Request
import org.json.JSONObject
import ro.srth.lbv2.command.LBCommand

@Suppress("unused")
class RandomRobloxGameCommand(data: Data) : LBCommand(data) {
    private companion object {
        const val APIPREFIX = "https://games.roblox.com/v2/users/"
        const val APISUFFIX = "/games?accessFilter=2&limit=50"
        const val SIMPLEPREFIX = "https://www.roblox.com/games/"

        const val MAX_REQUESTS = 10
    }

    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val user = event.getOption("userid", OptionMapping::getAsString)

        if (user == null) {
            selectRandomUser(event)
        } else {
            getRandomUserGame(user, event)
        }
    }

    private fun getRandomUserGame(user: String, event: SlashCommandInteractionEvent) {
        val request = Request.Builder()
            .url(APIPREFIX + user + APISUFFIX)
            .build()

        val call = bot.client.newCall(request)
        val response = call.execute()

        if (response.isSuccessful) {
            val data = JSONObject(response.body!!.string())

            val arr = data.getJSONArray("data")

            val index = bot.rand().nextInt(0, arr.length())

            val game = arr.getJSONObject(index).getJSONObject("rootPlace").getLong("id")

            event.reply(SIMPLEPREFIX + game).queue()
        } else {
            event.reply("Unable to get games, user may not exist.").queue()
        }

        response.close()
    }

    private fun selectRandomUser(event: SlashCommandInteractionEvent) {
        var randId = bot.rand().nextLong(0, 400000000)

        for (i in 0..MAX_REQUESTS) {
            val request = Request.Builder()
                .url(APIPREFIX + randId + APISUFFIX)
                .build()

            val call = bot.client.newCall(request)
            val response = call.execute()

            when {
                response.code != 404 -> {
                    val data = JSONObject(response.body!!.string())

                    val arr = data.getJSONArray("data")

                    if (arr.isEmpty) {
                        randId = bot.rand().nextLong(0, 400000000)
                        response.close()
                        continue
                    }

                    val index = bot.rand().nextInt(0, arr.length())

                    val game = arr.getJSONObject(index).getJSONObject("rootPlace").getLong("id")

                    event.reply("$SIMPLEPREFIX$game").queue()
                    
                    return
                }

                else -> {
                    randId = bot.rand().nextLong(0, 400000000)
                }
            }
            response.close()
        }

        event.reply("Failed to find a valid game after 10 requests, please try again.").queue()
    }
}