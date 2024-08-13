import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author guiyi
 * @Date 2024/8/13 下午10:07:57
 * @ClassName com.starseaoj.starseaojcodesandbox.unsafe.WriteFileError
 * @function --> 写文件，植入木马
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";

        // 创建bat文件，将恶意代码如删除文件等写入
        String errorProgram = "this is bad code";
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));

        // 使用终端命令运行bat文件...
        System.out.println("运行bat文件");
    }
}
