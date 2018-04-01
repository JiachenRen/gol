import jui.TextInput;

//import java.io.FileNotFoundException;
//import java.io.PrintWriter;
//import java.io.UnsupportedEncodingException;
//import java.net.URISyntaxException;
//import java.util.Scanner;

/**
 * Created by Jiachen on 6/14/17.
 * converts a #R file to standard #config file in Jiachen's Game of Life implementation
 */
public class ConvertToStandardConfig {
    public static void main(String args[]) {
        String lines = TextInput.getStringFromClipboard(true);
        int maxRow = 0, maxCol = 0;
        String incrementer = "", data = "";
        for (int i = 0; i < lines.length(); i++) {
            char c = lines.charAt(i);
            if (c != '\n')
                incrementer += c;
            else {
                String pos[] = incrementer.split(" ");
                int row = Integer.valueOf(pos[0]);
                int col = Integer.valueOf(pos[1]);
                maxRow = row > maxRow ? row : maxRow;
                maxCol = col > maxCol ? col : maxCol;
                data += row + "," + col + ";";
                incrementer = "";
            }
        }
        //Scanner scanner = new Scanner(System.in);
        //String fileName = scanner.nextLine();
        //try {
        //String folder = ConvertToStandardConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        //PrintWriter writer = new PrintWriter(folder + "/src/game_objs/configs/" + fileName, "UTF-8");
        System.out.println("#configs");
        System.out.println("~ dim:" + maxRow + "," + maxCol);
        System.out.println("~ pos:" + data);
        //System.out.close();
        //} catch (URISyntaxException | FileNotFoundException | UnsupportedEncodingException e) {
        //    e.printStackTrace();
        //}
    }
}
