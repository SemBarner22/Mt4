package nongen;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static nongen.MainParser.createCodeFile;
import static nongen.MainParser.toName;

public class HelperParser {

    private int index = 0;

    public void print(MainParser descriptor) throws IOException {
        String nested = makeNestedContent(descriptor);
        String functions = makeFunctions(descriptor);
        String result = nested + functions;
        String content = String.format(template, result);
        createCodeFile("Parser", content);
    }

    private String makeNestedContent(MainParser descriptor) {
        String nestedNodes = descriptor.idents.stream().map(node -> getNodesPrint(descriptor, node)).collect(Collectors.joining("\n"));
        String nestedLeaves = descriptor.terms.stream().map(node -> getLeavesPrint(descriptor, node)).collect(Collectors.joining("\n"));
        return nestedNodes + "\n" + nestedLeaves + "\n";
    }

    private String makeFunctions(MainParser descriptor) {
        String nodeFunctions = descriptor.idents.stream().map(n -> makeFunctionNode(descriptor, n)).collect(Collectors.joining("\n")) + "\n";
        String leafFunctions = descriptor.terms.stream().map(leaf -> makeFunctionLeaf(descriptor, leaf)).collect(Collectors.joining("\n")) + "\n";
        return nodeFunctions + "\n" + leafFunctions;
    }

    private String makeFunctionLeaf(MainParser descriptor, MainParser.Term leaf) {
        String funTemplate = """
                    public %sContext parse%s() {
                        %sContext result =  new %sContext();
                        result.text = data.get(position++).text;
                        return result;
                    }
                """;
        String name = toName(leaf.name);
        return String.format(funTemplate, name, name, name, name);
    }

    private String makeFunctionNode(MainParser descriptor, MainParser.Identifier node) {
        String funTemplate = """
                    public %sContext parse%s() {
                        %s
                    }
                """;
        String name = toName(node.name);

        String content = makeFunctionContent(descriptor, node);

        return String.format(funTemplate, name, name, content);
    }


    private String makeFunctionContent(MainParser descriptor, MainParser.Identifier node) {
        index = 0;
        String resultType = toName(node.name);
        String switchTemplate = """
                switch(data.get(position).terms) {
                    %s
                }
                %s
                """;
        String caseTemplate = """
                    case %s:
                        %sContext result%s = new %sContext();
                        %s
                        return result%s;
                """;
        StringBuilder builder = new StringBuilder();
        for (String termName : node.first) {
            if (!termName.equals("")) {
                builder.append(String.format(caseTemplate, termName, resultType, index, resultType, makeCaseForRule(descriptor, node, termName), index));
                index++;
            }
        }
        boolean needsException = false;
        if (node.first.contains("")) {
            processDefaultBlock(node, builder);
        } else {
            needsException = true;
        }
        String exception = needsException ? "throw new IllegalStateException(\"Unexpected token \" + data.get(position).terms.name() + \" at position: \" + position);" : "";
        return String.format(switchTemplate, builder.toString(), exception);
    }

    private void processDefaultBlock(MainParser.Identifier node, StringBuilder builder) {
        String defaultTemplate = """
                default:
                    %sContext result%s = new %sContext();
                    result%s.localIndexRule = %s;
                    return result%s;                    
                """;
        int number = 0;
        for (int i = 0; i < node.rules.size(); i++) {
            List<String> toList = node.rules.get(i).right;
            if (!toList.isEmpty()
                    && toList.get(0).equals("")
                    ) {
                number = i;
                break;
            }
        }
        String name = toName(node.name);
        builder.append(String.format(defaultTemplate, name, index, name, index, number, index));
    }

    private String makeCaseForRule(MainParser descriptor, MainParser.Identifier node, String termName) {
        MainParser.Rule nextRule = null;
        int number = 0;
        for (MainParser.Rule rule : node.rules) {
            String nextStateName = rule.right.get(0);
//            String nextStateName = null;
            Set<String> firstByNextState = descriptor.first.get(nextStateName);
            if (firstByNextState == null && nextStateName.equals(termName)) {
                nextRule = rule;
                break;
            }
            if (firstByNextState != null && firstByNextState.contains(termName)) {
                nextRule = rule;
                break;
            }
            number++;
        }
        if (nextRule == null) {
            throw new IllegalStateException("null");
        }
        String settingNumber = "result" + index + ".localIndexRule = " + number + ";\n";
        return settingNumber + nextRule.right.stream().map(toRuleName -> {
            Integer count = node.fieldCounter.get(toRuleName);
            if (count == null || count == 1) {
                return "result" + index + "." + toRuleName + " = parse" + toName(toRuleName) + "();";
            } else {
                return "result" + index + "." + toRuleName + ".add(parse" + toName(toRuleName) + "());";
            }
        }).collect(Collectors.joining("\n"));
    }

    private static final String template = """
            package generated;
                        
            import java.util.*;
                        
            public class Parser {
                        
                private int position = 0;
                
                private List<Tokens> data;
                
                public Parser(List<Tokens> data) {
                    this.data = data;
                }
                
                %s
                        
            }
            """;

    public String getNodesPrint(MainParser descriptor, MainParser.Identifier node) {
        String template = """
                    public static class %sContext {
                        
                        private int localIndexRule = -1;
                        
                        public String text;
                    
                        %s
                    
                        public void process() {
                            %s
                        }
                    
                    }
                """;
        String name = toName(node.name);
        String declaration = node.initVariables;
        String fields = makeFields(node);
        return String.format(template, name, declaration + "\n" + fields, makeProcess(descriptor, node));
    }

    private String makeProcess(MainParser descriptor, MainParser.Identifier node) {
        StringBuilder calling = new StringBuilder();
        for (Map.Entry<String, Integer> e : node.fieldCounter.entrySet()) {
            String name = e.getKey();
            if (descriptor.terms.stream().anyMatch(l -> l.name.equals(name))) {
                continue;
            }
            if (e.getValue() == 1) {
                calling.append("if (").append(name).append(" != null) {\n");
                calling.append(makeExtendedCode(descriptor, name)).append("\n");
                calling.append(name).append(".process(); \n}\n");
            } else {
                calling.append(name).append(".stream()\n.filter(Object::notNull)\n.forEach(x -> x.process());\n");
            }
        }

        String switchTemplate = """
                switch(localIndexRule) {
                    %s
                }
                """;
        String caseTemplate = """
                case %s:
                    %s
                    break;
                """;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < node.rulesCode.size(); i++) {
            builder.append(String.format(caseTemplate, i, node.rulesCode.get(i)));
        }
        return calling.toString() + String.format(switchTemplate, builder.toString()) + "\n";
    }

    private String makeExtendedCode(MainParser descriptor, String name) {
        MainParser.Identifier toIdentifier = null;
        Optional<MainParser.Identifier> child = descriptor.idents.stream().filter(node -> node.name.equals(name)).findFirst();
        if (child.isPresent()) {
            MainParser.Identifier childIdentifier = child.get();
            return childIdentifier.naslCode.replace("this", name);
        } else {
            return "";
        }
    }

    private String makeFields(MainParser.Identifier node) {
        return node.fieldCounter.entrySet().stream().map(e -> {
            String name = e.getKey();
            Integer count = e.getValue();
            return count == 1 ? "public " + toName(name) + "Context " + name + ";\n" : "private List<" + toName(name) + "Context> = new ArrayList<>();" + name + ";\n";
        }).collect(Collectors.joining());
    }

    public String getLeavesPrint(MainParser descriptor, MainParser.Term terms) {
        String template = """
                    public static class %sContext {
                    
                        public String text;
                    
                    }
                """;
        String name = toName(terms.name);
        return String.format(template, name);
    }

}

