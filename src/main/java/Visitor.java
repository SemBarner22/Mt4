import nongen.MainParser;
import org.antlr.v4.runtime.RuleContext;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Visitor {

    public List<String> input;

    public MainParser mainParser;

    public Visitor(MetagrammarParser.MetagrammarContext metagrammar) {
        var a = metagrammar.expr().size();
        List<MainParser.Term> terms = metagrammar.term().stream().map(this::getTerm).collect(Collectors.toList());
        List<MainParser.Identifier> ident = metagrammar.expr().stream().map(this::getIdent).collect(Collectors.toList());
        this.mainParser = new MainParser(terms, ident);
        List<MainParser.Rule> rules = mainParser.idents.stream().flatMap(r -> r.rules.stream()).collect(Collectors.toList());
        mainParser.allRules = rules;
        Map<String, Set<String>> first =  new HashMap<>();
        Map<String, Set<String>> follow = new HashMap<>();
        Predicate<String> isterms = (data) -> mainParser.terms.stream().anyMatch(d -> d.name.equals(data)) || data.equals("");
        mainParser.idents.forEach(node -> first.put(node.name, new HashSet<>()));
        mainParser.first = first;

        boolean changed = true;
        while (changed) {
            changed = false;
            for (MainParser.Rule currentRule : rules) {
                Set<String> setFrom = first.get(currentRule.left);
                if (!currentRule.right.isEmpty()) {
//                    String nextRuleName = null;
                    String nextRuleName = currentRule.right.get(0);
                    if (isterms.test(nextRuleName)) {
                        if (!setFrom.contains(nextRuleName)) {
                            changed = true;
                            setFrom.add(nextRuleName);
                        }
                    } else {
                        Set<String> setTo = first.get(nextRuleName);
                        boolean result = setFrom.addAll(setTo);
                        if (result) {
                            changed = true;
                        }
                    }
                }
            }
        }
        Predicate<String> isNode = (data) -> mainParser.idents.stream().anyMatch(d -> d.name.equals(data));
        Predicate<String> isterms1 = (data) -> mainParser.terms.stream().anyMatch(d -> d.name.equals(data)) || data.equals("");

//        follow.get("expr").add("$");
        mainParser.idents.forEach(node -> follow.put(node.name, new HashSet<>()));

        boolean changed1 = true;
        while (changed1) {
            changed1 = false;
            for (MainParser.Rule ruleA : rules) {
                String aName = ruleA.left;
                for (int i = 0; i < ruleA.right.size(); i++) {
                    String bName = ruleA.right.get(i);
//                    String bName = null;
                    if (isNode.test(bName)) {
                        Set<String> bSet = follow.get(bName);
                        if (i < ruleA.right.size() - 1) {
//                            String gammaName = null;
                            String gammaName = ruleA.right.get(i + 1);
                            Set<String> gammaFirst = new HashSet<>();
                            if (isterms1.test(gammaName)) {
                                gammaFirst.add(gammaName);
                            } else {
                                gammaFirst = first.get(gammaName);
                            }
                            boolean result = bSet.addAll(gammaFirst);
                            if (gammaFirst.contains("")) {
                                result |= bSet.addAll(follow.get(aName));
                            }
                            changed1 |= result;
                        } else {
                            changed1 |= bSet.addAll(follow.get(aName));
                        }
                    }
                }
            }
        }
        for (var node: mainParser.idents) {
            String name = node.name;
            node.first = first.get(name);
            node.follow = follow.get(name);
        }
    }

    private MainParser.Identifier getIdent(MetagrammarParser.ExprContext expressionContext) {
        String name = expressionContext.IDENT().getText();
        input = new ArrayList<>();
        List<MainParser.Rule> rules = visitDeclaration(name, expressionContext.decl());

        MetagrammarParser.AContext attr_initContext = expressionContext.a();
        String init = "";
        if (attr_initContext != null && attr_initContext.EXPR() != null) {
            init = formatContent(attr_initContext.EXPR().getText());
        }
        MetagrammarParser.LaContext attr_parentContext = expressionContext.la();
        String parValue = "";
        if (attr_parentContext != null) {
            parValue = formatContent(attr_parentContext.EXPR().getText());
        }
        MainParser.Identifier node = new MainParser.Identifier(name, init, rules, input, parValue);
        if (expressionContext.empty() != null) {
            node.empty = true;
            rules.add(new MainParser.Rule(name, List.of("")));
        }
        return node;
    }

    private List<MainParser.Rule> visitDeclaration(String name, MetagrammarParser.DeclContext declaration) {
        return declaration.declOne().stream().map(x -> parseOneRule(name, x)).collect(Collectors.toList());
    }

    private MainParser.Rule parseOneRule(String name, MetagrammarParser.DeclOneContext context) {
        if (context.sa() != null) {
            input.add(formatContent(context.sa().EXPR().getText()));
        }
        return parseChain(name, context.vars());
    }

    private MainParser.Rule parseChain(String name, MetagrammarParser.VarsContext chain) {
        return new MainParser.Rule(name, chain.var().stream()
                .map(RuleContext::getText)
                .collect(Collectors.toList()));
    }


    private MainParser.Term getTerm(MetagrammarParser.TermContext context) {
        String name = context.TERM().getText();
        String regexp = formatContent(context.EXPR().getText());
        return new MainParser.Term(name, regexp);
    }

    public static String formatContent(String content) {
        int len = content.length();
        return content.substring(1, len - 1);
    }

}
