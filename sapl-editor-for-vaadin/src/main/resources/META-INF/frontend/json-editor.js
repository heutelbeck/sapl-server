import { LitElement, html } from 'lit-element';
import { CodeMirrorStyles, CodeMirrorLintStyles, CodeMirrorHintStyles, HeightFix, ReadOnlyStyle } from './shared-styles.js';

class JSONEditor extends LitElement {

  constructor() {
    super();
    this.document = "";
  }

  static get properties() {
    return {
      document: { type: String },
      isReadOnly: { type: Boolean }, 
      hasLineNumbers: { type: Boolean },
      autoCloseBrackets: { type: Boolean },
      matchBrackets: { type: Boolean },
      textUpdateDelay: { type: Number },
      editor: { type: Object },
    }
  }

  static get styles() {
    return [
      CodeMirrorStyles,
      CodeMirrorLintStyles,
      CodeMirrorHintStyles,
      HeightFix,
      ReadOnlyStyle,
    ]
  }

  set editor(value) {
    let oldVal = this._editor;
    this._editor = value;
    console.debug('JsonEditor: set editor', oldVal, value);
    this.requestUpdate('editor', oldVal);
    this.onEditorChangeCheckOptions(value);
  }

  get editor() {
    return this._editor;
  }

  set isReadOnly(value) {
    let oldVal = this._isReadOnly;
    this._isReadOnly = value;
    console.debug('JsonEditor: set isReadOnly', oldVal, value);
    this.requestUpdate('isReadOnly', oldVal);
    this.setEditorOption('readOnly', value);
  }

  get isReadOnly() { 
    return this._isReadOnly; 
  }

  connectedCallback() {
    super.connectedCallback();

    var self = this;
    var shadowRoot = self.shadowRoot;

    require(["codemirror",
      "codemirror/mode/javascript/javascript",
      "./jsonlint",
      "codemirror/addon/lint/lint",
      "codemirror/addon/lint/json-lint",
      ], 
      function (codemirror, mode, jsonlint, cmlint, cmjsonlint) {
        
        window.jsonlint = jsonlint;

        self.editor = codemirror(shadowRoot, {
          value: self.document,
          mode: "application/json",
          gutters: ["CodeMirror-lint-markers"],
          readOnly: self.isReadOnly,
          lineNumbers: self.hasLineNumbers,
          showCursorWhenSelecting: true,
          textUpdateDelay: self.textUpdateDelay,
          lint: {
            selfContain: true
          },
        });

        self.editor.on("change", function(cm, changeObj) {
          var value = cm.getValue();
          self.onDocumentChanged(value);
        });
      });
  }

  onDocumentChanged(value) {
    this.document = value;
    if(this.$server !== undefined) {
      this.$server.onDocumentChanged(value);
    }
    else {
      throw "Connection between editor and server could not be established. (onDocumentChanged)";
    }
  }

  // ServerCallable
  setEditorDocument(element, document) {
    this.document = document;
    if(element.editor !== undefined) {
      element.editor.doc.setValue(document);
    }
  }

  setEditorOption(option, value) {
    let isEditorSet = this.editor !== undefined;
    console.debug('JsonEditor: setEditorOption', option, value, isEditorSet);

    if(this.editor !== undefined) {
      if(option === 'readOnly') {
        if(value === true) {
          this.editor.setOption("theme", 'readOnly');
        } else {
          this.editor.setOption("theme", 'default');
        }
      }
      this.editor.setOption(option, value);  
    }
  }

  onEditorChangeCheckOptions(editor) {
    let isEditorSet = editor !== undefined;
    console.debug('JsonEditor: onEditorChangeCheckOptions', isEditorSet);

    if(isEditorSet) {
      this.setEditorOption('readOnly', this.isReadOnly);
    }
  }

  // ServerCallable
  onRefreshEditor() {
    let isEditorSet = this.editor !== undefined;
    console.debug('JsonEditor: onRefreshEditor', isEditorSet);

    if(isEditorSet) {
      console.debug('JsonEditor: refresh Editor');
      this.editor.refresh();
    }
  }

  render() {
    return html``;
  }
}

customElements.define('json-editor', JSONEditor);