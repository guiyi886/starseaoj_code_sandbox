/**
 * @author guiyi
 * @Date 2024/8/13 下午8:55:31
 * @ClassName com.starseaoj.starseaojcodesandbox.unsafe.SleepError
 * @function --> 无限睡眠（阻塞程序运行）
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        long ONE_HOUR = 60 * 60 * 1000L;
        Thread.sleep(ONE_HOUR);
        System.out.println("睡完了");
    }
}
