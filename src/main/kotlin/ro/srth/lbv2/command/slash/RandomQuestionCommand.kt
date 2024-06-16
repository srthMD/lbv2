package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessagePollData
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.io.File
import java.util.concurrent.TimeUnit

class RandomQuestionCommand(data: Data) : LBCommand(data) {
    private val canRun: Boolean
    private val questions: List<List<String>>?

    private val rand = Bot.getInstance().rand()

    init {
        val dir = File("cmds/randomquestion")

        if (!dir.exists()) {
            canRun = false
            questions = null
        } else {
            questions = ArrayList()
            dir.listFiles()?.forEach {
                questions.add(it.readLines())
            }
            canRun = true
        }
    }

    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        if (!canRun) {
            event.reply("This command is not available right now.").setEphemeral(true).queue()
            return
        }

        val hours = event.getOption("minutes")?.asLong ?: 1

        val genre = questions?.get(rand.nextInt(questions.size))
        val start = genre?.get(0)

        val first = rand.nextInt(1, genre!!.size)
        var second = rand.nextInt(1, genre.size)

        while (first == second) {
            second = rand.nextInt(1, genre.size)
        }

        val firstAnswer = genre[first].split(":")
        val secondAnswer = genre[second].split(":")

        val firstEmoji = if (firstAnswer.size > 1) firstAnswer.last() else "❔"
        val secondEmoji = if (secondAnswer.size > 1) secondAnswer.last() else "❔"

        val action = event.reply("$start ${firstAnswer.first()} or ${secondAnswer.first()}?")

        action.setPoll(
            MessagePollData.builder("Pick a choice")
                .addAnswer(firstAnswer.first(), Emoji.fromFormatted(firstEmoji))
                .addAnswer(secondAnswer.first(), Emoji.fromFormatted(secondEmoji))
                .setDuration(hours, TimeUnit.HOURS)
                .build()
        )

        action.queue()
    }
}