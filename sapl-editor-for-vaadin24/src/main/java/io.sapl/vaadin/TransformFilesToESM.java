package io.sapl.vaadin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformFilesToESM {

    public static void main(String[] args) {
        String targetFolderPath = System.getProperty("user.dir") + "/target/classes/META-INF/frontend/";

        convertFileToESM(targetFolderPath + "sapl-mode.js", TransformFilesToESM::convertSaplModeToESM);
        convertFileToESM(targetFolderPath + "xtext-codemirror.js", TransformFilesToESM::convertXtextCodemirrorToESM);
    }

    private static void convertFileToESM(String filePath, Converter converter) {
        File file = new File(filePath);

        if (file.isFile()) {
            try {
                String content = Files.readString(file.toPath());
                String result = converter.convert(content);
                Files.write(file.toPath(), result.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String convertSaplModeToESM(String sourceCode) {
        String namedDefineRegex = "define\\((['\"])([^'\"]+)\\1,\\s*\\[([^\\]]*)\\]\\s*,\\s*function\\s*\\(([^)]*)\\)\\s*\\{";
        List<String> importStatements = new ArrayList<>();
        Pattern pattern = Pattern.compile(namedDefineRegex);
        Matcher matcher = pattern.matcher(sourceCode);
        StringBuilder esmSourceCode = new StringBuilder();
        while (matcher.find()) {
            String moduleName = matcher.group(2);
            String dependencies = matcher.group(3).replaceAll("\\s+", "");
            String args = matcher.group(4).replaceAll("\\s+\\{", "");
            String[] dependencyArray = dependencies.split(",");
            String[] argArray = args.split(",");
            StringBuilder importBuilder = new StringBuilder();
            for (int i = 0; i < dependencyArray.length; i++) {
                String dependency = dependencyArray[i];
                String arg = argArray[i];
                importBuilder.append("import ").append(arg).append(" from ").append(dependency).append(";\n");
            }
            importStatements.add(importBuilder.toString());
            matcher.appendReplacement(esmSourceCode, "");
        }
        matcher.appendTail(esmSourceCode);
        esmSourceCode.insert(0, String.join("", importStatements));
        esmSourceCode.delete(esmSourceCode.lastIndexOf("});"), esmSourceCode.length());
        return esmSourceCode.toString();
    }

    private static String convertXtextCodemirrorToESM(String code) {
        // Define regex to match define functions with dependencies
        String defineRegex = "define\\(('|\")[^('|\\\")]*('|\"),(\\s*\\[[^\\]]*?\\])?(\\s*,\\s*)?function(\\s*)\\(([^)]*)\\)(\\s*)\\{";

        // Remove all define functions with dependencies
        String esmContent = code.replaceAll(defineRegex, "");

        String returnRegex = "return(\\s+)[A-Z][a-z]+[a-zA-Z]+;[\\s]+}\\);|return(\\s+)exports;[\\s]+}\\);|return\\s\\{\\};\\s}\\);";
        esmContent = esmContent.replaceAll(returnRegex, "");

        // Rename 'CodeMirrorEditorContext' to 'EditorContext'
        esmContent = esmContent.replaceAll("CodeMirrorEditorContext", "EditorContext");

        // Add imports at the beginning
        String imports = "import jQuery from 'jquery';\n" +
                "import CodeMirror from 'codemirror';\n" +
                "import ShowHint from 'codemirror/addon/hint/show-hint.js';\n" +
                "import 'codemirror/mode/javascript/javascript.js';\n\n";
        esmContent = imports + esmContent;

        // Add exports at the end
        String exports = "export { exports, ServiceBuilder, EditorContext, XtextService, LoadResourceService, SaveResourceService, HighlightingService, ValidationService, UpdateService, ContentAssistService, HoverService, OccurrencesService, FormattingService };\n";
        esmContent = esmContent + exports;

        return esmContent;
    }

    private interface Converter {
        String convert(String content);
    }

}
