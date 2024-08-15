/**
 * @author guiyi
 * @Date 2024/8/13 上午12:33:03
 * @ClassName com.starseaoj.starseaojcodesandbox.Main
 * @function --> 测试终端命令行编译运行，注意不能加包名
 */
public class Main {
    public static void main(String[] args) {
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        System.out.println("结果为" + (a + b));
    }
}
