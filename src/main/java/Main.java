//import generated.Lexer;
//import generated.Parser;
//import generated.Tokens;
import nongen.HelperParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    public static void main(@org.jetbrains.annotations.NotNull String[] args) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String data;
        try (InputStream is = loader.getResourceAsStream("lab2.a4")) {
            data = new String(Objects.requireNonNull(is).readAllBytes());
        }
        var lexer = new MetagrammarLexer(CharStreams.fromString(data));
        TokenStream tokens = new CommonTokenStream(lexer);
        MetagrammarParser parser = new MetagrammarParser(tokens);
        var gram = new Visitor(parser.metagrammar());
        var metagram = gram.mainParser;
        if (metagram == null) {
            return;
        }
        metagram.generateLexer();
        metagram.generateTokens();
        metagram.generateEnum();
        new HelperParser().print(metagram);

//        String input = "void _A ( int _B ) ;";
//        Lexer lexerS = new Lexer(input);
//        List<Tokens> tokenList = lexerS.parseAll();
//        System.out.println(tokenList);
////        var exprContext = new Parser(tokenList).parseExpr();
//        var exprContext = new Parser(tokenList).parseS();
//        exprContext.process();
//        System.out.println(exprContext.res);
//        Scanner scanner = new Scanner(System.in);
//        while (true) {
//            System.out.println(calc(scanner.nextLine()));
//        }
//    }
//
//    private static double calc(String input) {
//        Lexer lexerS = new Lexer(input);
//
//        List<Tokens> tokenList = lexerS.parseAll();
//        System.out.println(tokenList);
////        var exprContext = new Parser(tokenList).parseExpr();
//        var exprContext = new Parser(tokenList).parseS();
//        exprContext.process();
//        return exprContext.res;
    }
}
