package ro.srth.lbv2.util;

import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ImageUtils {
    @Nullable
    public static BufferedImage attachmentToImage(Message.Attachment img) {
        var proxy = img.getProxy();

        try {
            return ImageIO.read(proxy.download().get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            return null;
        }
    }
}
