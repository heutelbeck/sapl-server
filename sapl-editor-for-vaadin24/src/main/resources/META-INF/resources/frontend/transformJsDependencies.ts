import { Plugin } from 'vite';

export default function transformJsDependencies(): Plugin {
    return {
        name: 'transformJsDependencies',
        transform(code, id) {
            if (id.endsWith("sapl-mode.js")) {
                console.log("Transform sapl-mode.js");
                return convertSaplModeToESM(code);
            }
            else if (id.endsWith("xtext-codemirror.js")) {
                console.log("Transform xtext-codemirror.js");
                return convertXtextCodemirrorToESM(code);
            } else {
                return code;
            }
        },
    };
}

function convertSaplModeToESM(sourceCode: string): string {
    const namedDefineRegex = /define\((['"])([^'"]+)\1,\s*\[([^\]]*)\]\s*,\s*function\s*\(([^)]*)\)\s*\{/g;

    const importStatements: string[] = [];
    let esmSourceCode = sourceCode.replace(namedDefineRegex, (match, quote, moduleName, dependencies, args) => {
        const dependencyArray = dependencies.split(',').map((dependency: string) => dependency.trim());
        const argArray = args.split(',').map((arg: string) => arg.trim());
        const importArray: string[] = [];

        for (let i = 0; i < dependencyArray.length; i++) {
            const dependency = dependencyArray[i];
            const arg = argArray[i];
            importArray.push(`import ${arg} from ${dependency};`);
        }

        importStatements.push(...importArray);
        return '';
    });

    esmSourceCode = `${importStatements.join('\n')}\n${esmSourceCode}`;
    esmSourceCode = esmSourceCode.replace(/}\);\s*$/, '');

    return esmSourceCode;
}

function convertXtextCodemirrorToESM(code: string): string {
    // Define regex to match define functions with dependencies
    const defineRegex = /define\(('|")[^('|")]*('|"),(\s*\[[^[]*?\])?(\s*,\s*)?function(\s*)\(([^)]*)\)(\s*){/g;

    // Remove all define functions with dependencies
    let esmContent = code.replace(defineRegex, '');

    const returnRegex = /return(\s+)[A-Z][a-z]+[a-zA-Z]+;[\s]+}\);|return(\s+)exports;[\s]+}\);|return\s{};\s}\);/g;
    esmContent = esmContent.replace(returnRegex, '');

    // Rename 'CodeMirrorEditorContext' to 'EditorContext'
    esmContent = esmContent.replace(/CodeMirrorEditorContext/g, 'EditorContext');

    // Add imports at the beginning
    const imports = `import jQuery from 'jquery';
    import CodeMirror from 'codemirror';
    import ShowHint from 'codemirror/addon/hint/show-hint.js';
    import 'codemirror/mode/javascript/javascript.js';\n\n`;
    esmContent = imports + esmContent;

    // Add exports at the end
    const exports = `export { exports, ServiceBuilder, EditorContext, XtextService, LoadResourceService, SaveResourceService, HighlightingService, ValidationService, UpdateService, ContentAssistService, HoverService, OccurrencesService, FormattingService };\n`;
    esmContent = esmContent + exports;

    return esmContent;
}
