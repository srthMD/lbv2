package ro.srth.lbv2.util

import net.dv8tion.jda.api.entities.Message
import java.awt.image.BufferedImage

public fun Message.Attachment.toImage(): BufferedImage? {
    return ImageUtils.attachmentToImage(this);
}
