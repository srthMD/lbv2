package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessagePollBuilder
import net.dv8tion.jda.api.utils.messages.MessagePollData
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.io.File
import java.time.Duration

private const val QUESTIONMARK = "❔"

@Suppress("unused")
class RandomQuestionCommand(data: Data) : LBCommand(data) {
    private val canRun: Boolean
    private val questions: List<List<String>>?

    private val questionMarkEmoji = Emoji.fromUnicode(QUESTIONMARK)

    private val rand = Bot.getInstance().rand()

    init {
        val dir = File("cmds/randomquestion")

        if (!dir.exists()) {
            canRun = false
            questions = null
        } else {
            questions = ArrayList()
            dir.listFiles()?.forEach {
                val strs: ArrayList<String> = ArrayList()

                it.forEachLine { line ->
                    if (line.startsWith("#") || line.isBlank()) {
                        return@forEachLine
                    }

                    strs.add(line)
                }
                questions.add(strs)
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

        val question = questions!![rand.nextInt(questions.size)]
        val mode = question[0]

        val action = when (mode) {
            "MC" -> multipleChoiceBuilder(question)

            "TF" -> trueFalseBuilder(question)

            else -> {
                event.reply("Something went wrong generating the question.").setEphemeral(true).queue()
                return
            }
        }


        action.setDuration(Duration.ofHours(hours))

        event.replyPoll(action.build()).queue()
    }

    private fun multipleChoiceBuilder(question: List<String>): MessagePollBuilder {
        val template = question[1]

        val strs = template.split("%s")

        //tmp set to track duplicate options
        val indexSet = hashSetOf<String>()

        val action = MessagePollData.builder("null")
        val title = StringBuilder()

        strs.forEachIndexed { i, str ->
            var indx = question[rand.nextInt(2, question.size)]

            if (i == strs.size - 1) {
                title.append(str)
                return@forEachIndexed
            }

            if (i == 0) {
                indexSet.add(indx)
            } else {
                while (indexSet.contains(indx)) {
                    indx = question[rand.nextInt(2, question.size)]
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
                action.addAnswer(first, questionMarkEmoji)
            }
        }

        action.setTitle(title.toString())
        return action
    }

    private fun trueFalseBuilder(question: List<String>): MessagePollBuilder {
        val trueAnswer = question[2].split(":")
        val falseAnswer = question[3].split(":")

        val strs = question[1].split("%s")

        val indexSet = hashSetOf<String>()
        val action = MessagePollData.builder("null")
        val title = StringBuilder()

        strs.forEachIndexed { i, str ->
            var indx = question[rand.nextInt(4, question.size)]

            if (i == strs.size - 1) {
                title.append(str)
                return@forEachIndexed
            }

            if (i == 0) {
                indexSet.add(indx)
            } else {
                while (indexSet.contains(indx)) {
                    indx = question[rand.nextInt(4, question.size)]
                }

                indexSet.add(indx)
            }

            val split = indx.split(":")
            val first = split.first()

            title.append(str + first)
        }

        if (trueAnswer.size > 1) {
            action.addAnswer(trueAnswer.first(), Emoji.fromUnicode(trueAnswer.last()))
            action.addAnswer(falseAnswer.first(), Emoji.fromUnicode(falseAnswer.last()))
        } else {
            action.addAnswer(trueAnswer.first(), questionMarkEmoji)
            action.addAnswer(falseAnswer.first(), questionMarkEmoji)
        }

        action.setTitle(title.toString())
        return action
    }
}