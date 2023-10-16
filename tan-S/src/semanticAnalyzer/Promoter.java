package semanticAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.PseudoOperator;
import parseTree.ParseNode;
import parseTree.nodeTypes.ArrayIndexNode;
import parseTree.nodeTypes.CastNode;
import parseTree.nodeTypes.NewArrayNode;
import parseTree.nodeTypes.OperatorNode;
import parseTree.nodeTypes.PopulatedArrayNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;

public class Promoter {

    LinkedHashMap<ParseNode, List<PrimitiveType>> promotions;

    public Promoter() {
        promotions = new LinkedHashMap<ParseNode, List<PrimitiveType>>();
    }

    private Lextant operatorFor(ParseNode node) {
        LextantToken token = (LextantToken) node.getToken();
        return token.getLextant();
    }

    public boolean promotable(OperatorNode node) {
        Lextant operator = operatorFor(node);

        List<Type> childTypes = new ArrayList<Type>();
        node.getChildren().forEach((child) -> childTypes.add(child.getType()));
        FunctionSignature signature = FunctionSignatures.signature(operator, childTypes);

        // cast to integer
        for (int i = 0; i < childTypes.size(); i++) {
            if (childTypes.get(i) == PrimitiveType.CHARACTER) {
                childTypes.set(i, PrimitiveType.INTEGER);
                signature = FunctionSignatures.signature(operator, childTypes);
                if (signature.isNull()) {
                    childTypes.set(i, PrimitiveType.CHARACTER);
                } else {
                    addPromotion(node.child(i), Arrays.asList(PrimitiveType.INTEGER));
                }
            }
        }

        if (signature.accepts(childTypes)) {
            node.setSignature(signature);
            node.setType(signature.resultType());
            return true;
        }

        // cast to float
        for (int i = 0; i < childTypes.size(); i++) {
            if (childTypes.get(i) == PrimitiveType.CHARACTER) {
                childTypes.set(i, PrimitiveType.FLOAT);
                signature = FunctionSignatures.signature(operator, childTypes);
                if (signature.isNull()) {
                    childTypes.set(i, PrimitiveType.CHARACTER);
                } else {
                    addPromotion(node.child(i), Arrays.asList(PrimitiveType.INTEGER, PrimitiveType.FLOAT));
                }
            }

            if (childTypes.get(i) == PrimitiveType.INTEGER) {
                childTypes.set(i, PrimitiveType.FLOAT);
                signature = FunctionSignatures.signature(operator, childTypes);
                if (signature.isNull()) {
                    childTypes.set(i, PrimitiveType.INTEGER);
                } else {
                    addPromotion(node.child(i), Arrays.asList(PrimitiveType.FLOAT));
                }
            }
        }

        if (signature.accepts(childTypes)) {
            node.setSignature(signature);
            node.setType(signature.resultType());
            return true;
        }

        return false;
    }

    public boolean promotable(PopulatedArrayNode node) {
        List<Type> childTypes = new ArrayList<Type>();
        List<Type> castTypes = new ArrayList<Type>();

        node.getChildren().forEach((child) -> {
            Type type = child.getType();
            childTypes.add(type);
            castTypes.add(type);
        });

        // cast to integer
        for (int i = 0; i < childTypes.size(); i++) {			
            if (childTypes.get(i) == PrimitiveType.CHARACTER) {
                castTypes.set(i, PrimitiveType.INTEGER);
                addPromotion(node.child(i), Arrays.asList(PrimitiveType.INTEGER));
            }
        }

        if (Collections.frequency(castTypes, PrimitiveType.INTEGER) == node.nChildren()) {
            node.setType(new ArrayType(PrimitiveType.INTEGER));
            return true;
        } else {
            promotions.clear();
            castTypes.clear();
            node.getChildren().forEach((child) -> castTypes.add(child.getType()));
        }

        // cast to float
        for (int i = 0; i < childTypes.size(); i++) {
            if (childTypes.get(i) == PrimitiveType.CHARACTER) {
                castTypes.set(i, PrimitiveType.FLOAT);
                addPromotion(node.child(i), Arrays.asList(PrimitiveType.INTEGER, PrimitiveType.FLOAT));
            }
            
            if (childTypes.get(i) == PrimitiveType.INTEGER) {
                castTypes.set(i, PrimitiveType.FLOAT);
                addPromotion(node.child(i), Arrays.asList(PrimitiveType.FLOAT));
            }
        }

        if (Collections.frequency(castTypes, PrimitiveType.FLOAT) == node.nChildren()) {
            node.setType(new ArrayType(PrimitiveType.FLOAT));
            return true;
        } else {
            promotions.clear();	
            castTypes.clear();
            node.getChildren().forEach((child) -> castTypes.add(child.getType()));
        }

        return false;
    }

    public boolean promotable(ArrayIndexNode node) {
        List<Type> childTypes = new ArrayList<Type>();
        node.getChildren().forEach((child) -> childTypes.add(child.getType()));

        if (childTypes.get(0) instanceof ArrayType) {
            if (childTypes.get(1) == PrimitiveType.CHARACTER) {
                addPromotion(node.child(1), Arrays.asList(PrimitiveType.INTEGER));
                Type subtype = ((ArrayType)childTypes.get(0)).getSubType();
                node.setType(subtype);
                return true;
            }
        }

        return false;
    }

    public boolean promotable(NewArrayNode node) {
        List<Type> childTypes = new ArrayList<Type>();
        node.getChildren().forEach((child) -> childTypes.add(child.getType()));

        if (childTypes.get(0) == PrimitiveType.CHARACTER) {
            addPromotion(node.child(0), Arrays.asList(PrimitiveType.INTEGER));
            node.setType(new ArrayType(PrimitiveType.INTEGER));
            return true;
        }

        return false;

    }

    public boolean promotable(ParseNode node) {
        Lextant operator = operatorFor(node);

        List<Type> childTypes = new ArrayList<Type>();
        node.getChildren().forEach((child) -> childTypes.add(child.getType()));
        FunctionSignature signature = FunctionSignatures.signature(operator, childTypes);

        // Check for cast to integer
        for (int i = 1; i < childTypes.size(); i++) {
            if (childTypes.get(i) == PrimitiveType.CHARACTER) {
                childTypes.set(i, PrimitiveType.INTEGER);
                signature = FunctionSignatures.signature(operator, childTypes);
                if (signature.isNull()) {
                    childTypes.set(i, PrimitiveType.CHARACTER);
                } else {
                    addPromotion(node.child(i), Arrays.asList(PrimitiveType.INTEGER));
                }
            }
        }

        if (signature.accepts(childTypes)) {
            node.setType(signature.resultType());
            return true;
        }

        // Check for cast to float
        for (int i = 1; i < childTypes.size(); i++) {
            if (childTypes.get(i) == PrimitiveType.CHARACTER) {
                childTypes.set(i, PrimitiveType.FLOAT);
                signature = FunctionSignatures.signature(operator, childTypes);
                if (signature.isNull()) {
                    childTypes.set(i, PrimitiveType.CHARACTER);
                } else {
                    addPromotion(node.child(i), Arrays.asList(PrimitiveType.INTEGER, PrimitiveType.FLOAT));
                }
            }

            if (childTypes.get(i) == PrimitiveType.INTEGER) {
                childTypes.set(i, PrimitiveType.FLOAT);
                signature = FunctionSignatures.signature(operator, childTypes);
                if (signature.isNull()) {
                    childTypes.set(i, PrimitiveType.INTEGER);
                } else {
                    addPromotion(node.child(i), Arrays.asList(PrimitiveType.FLOAT));
                }
            }
        }

        if (signature.accepts(childTypes)) {
            node.setType(signature.resultType());
            return true;
        }

        return false;
    }

    private void addPromotion(ParseNode node, List<PrimitiveType> castTypes) {
        promotions.put(node, castTypes);
    }

    public void promote() {
        for (Map.Entry<ParseNode, List<PrimitiveType>> entry : promotions.entrySet()) {
            ParseNode node = entry.getKey();
            List<PrimitiveType> casts = entry.getValue();

            for (PrimitiveType type : casts) {
                ParseNode parentNode = node.getParent();
                List<Type> childTypes = Arrays.asList(node.getType(), type);
                FunctionSignature signature = FunctionSignatures.signature(PseudoOperator.CAST, childTypes);
                CastNode cast = CastNode.withChildren(PseudoOperator.CAST.prototype(), type, node);
                cast.setSignature(signature);
                cast.setType(signature.resultType());
                parentNode.replaceChild(node, cast);
                node = cast;
            }
        }
    }

}
