package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessagePollData
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.io.File
import java.time.Duration

@Suppress("unused")
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

        val hours = event.getOption("hours")?.asLong ?: 1

        val genre = questions?.get(rand.nextInt(questions.size))
        val template = genre!![0]

        val strs = template.split("%s")

        //tmp set to track duplicate options
        val indexSet: HashSet<String> = hashSetOf()

        val action = MessagePollData.builder("null")
        val title = StringBuilder()

        strs.forEachIndexed { i, str ->
            var indx = genre[rand.nextInt(1, genre.size)]

            if (i == strs.size - 1) {
                title.append(str.trim())
                return@forEachIndexed
            }

            if (i == 0) {
                indexSet.add(indx)
            } else {
                while (indexSet.contains(indx)) {
                    indx = genre[rand.nextInt(1, genre.size)]
                }

                indexSet.add(indx)
            }

            val split = indx.split(":")

            val first = split.first()
            val emoji = Emoji.fromUnicode(split.last())

            title.append(str + first)

            if (split.size > 1) {
                action.addAnswer(first, emoji)
            } else {
                action.addAnswer(first, Emoji.fromUnicode("❔"))
            }
        }

        action.setTitle(title.toString())

        action.setDuration(Duration.ofHours(hours))

        event.replyPoll(action.build()).queue()
    }
}