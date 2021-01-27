package nongen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MainParser {

    public MainParser(List<Term> terms, List<Identifier> idents) {
        this.terms = terms;
        this.idents = idents;
    }

    public static class Term {

        public boolean empty = false;

        public String name;

        public String regex;

        public Term(String name, String regex) {
            this.name  =  name;
            this.regex = regex;
        }
    }

    public List<Term> terms;

    public static class Identifier {

        public String name;

        public boolean empty = false;

        public Set<String> first = new HashSet<>();

        public Set<String> follow = new HashSet<>();

        public final String initVariables;

        public final List<Rule> rules;

        public final List<String> rulesCode;

        public final String parVal;

        public final Map<String, Integer> fieldCounter;

        public final Map<String, Map<String, Integer>> rulesFieldCounter;

        public Identifier(String name, String initVariables, List<Rule> rules, List<String> rulesCode, String parVal) {
            this.name = name;
            this.initVariables = initVariables;
            this.rules = rules;
            this.rulesCode = rulesCode;
            this.parVal = parVal;
            fieldCounter = new HashMap<>();
            rulesFieldCounter = new HashMap<>();
            for (Rule rule : rules) {
                var tempMap = new HashMap<String, Integer>();
                for (String ruleName : rule.right) {
                    tempMap.merge(ruleName, 1, Integer::sum);
                }
                rulesFieldCounter.put(rule.left, tempMap);
                for (Map.Entry<String, Integer> entry : tempMap.entrySet()) {
                    fieldCounter.merge(entry.getKey(), entry.getValue(), Math::max);
                }
            }
        }
    }

    public List<Identifier> idents;

    public Map<String, Set<String>> first;

    public static class Rule {
        public String left;
        public List<String> right;

        public Rule(String left, List<String> right) {
            this.left = left;
            this.right = right;
        }
    }

    public List<Rule> allRules;


    public static void createCodeFile(String name, String code) throws IOException {
        Path genPath = Path.of("src/main/java/generated/" + name + ".java");
        Files.createFile(genPath);
        try (var buf = Files.newBufferedWriter(genPath)) { buf.write(code); }
    }

    public void generateTokens() throws IOException {
        String termsEnum = terms.stream().map(t -> t.name).collect(Collectors.joining(", "));
        String header = ""
                + "package generated;" + "\n"
                + "enum Terms {" + "\n"
                + "\t" + "END, " + termsEnum + "\n"
                + "}";
        createCodeFile("Terms", header);
    }

    public void generateLexer() throws IOException {
        String preTemplate = """
                package generated;
                import java.util.*;
                import java.util.regex.Matcher;
                import java.util.regex.Pattern;
                              
                public class Lexer {
                              
                    private String data;
                          
                    private final Map<Pattern, Terms> map;
                             
                    public Lexer(String data) {
                        this.data = data.replaceAll("[ \\n\\t]", "");
                        map = new LinkedHashMap<>();
                        %s
                    }
                               
                    public List<Tokens> parseAll() {
                        List<Tokens> tokens = new ArrayList<>();
                        while (true) {
                            Tokens current = getTokens();
                            tokens.add(current);
                            if (current.terms == Terms.END) {
                                return tokens;
                            }
                        }
                    }
                            
                    private Tokens getTokens() {
                        if (data.isEmpty()) {
                            return new Tokens(Terms.END);
                        }
                             
                        for (Map.Entry<Pattern, Terms> entry : map.entrySet()) {
                            Pattern pat = entry.getKey();
                            Terms terms = entry.getValue();
                            Matcher matcher = pat.matcher(data);
                            if (matcher.matches() && !matcher.group().isEmpty()) {
                                String result = matcher.group(1);
                                int len = result.length();
                                if (len < data.length()) {
                                    data = data.substring(len);
                                } else {
                                    data = "";
                                }
                                return new Tokens(terms, result);
                            }
                              
                        }
                             
                        return new Tokens(Terms.END);
                    }
                               
                }
                             
                """;
        String termsEnum = terms.stream().map(terms -> "map.put(Pattern.compile(\"(" + terms.regex + ").*\"), Terms." + terms.name + ");\n").collect(Collectors.joining());
        createCodeFile("Lexer", String.format(preTemplate, termsEnum));
   }


    public void generateEnum() throws IOException {
        String temp = """
                            
                package generated;
              
                          
                public class Tokens {
                                
                    public Terms terms;
                                
                    public String text = "";
                                
                    public Tokens(Terms terms, String text) {
                        this.terms = terms;
                        this.text = text;
                    }
                                
                    public Tokens(Terms terms) {
                        this.terms = terms;
                    }
                                
                    @Override
                    public String toString() {
                        return terms.name() + " : [" + text + "] ";
                    }
                }
                               
                                
                            
                """;
        createCodeFile("Tokens", temp);
    }


    public static String toName(String v) {
        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }


}
