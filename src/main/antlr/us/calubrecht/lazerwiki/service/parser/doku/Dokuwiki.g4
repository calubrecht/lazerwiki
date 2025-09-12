grammar Dokuwiki;

@header {
package us.calubrecht.lazerwiki.service.parser.doku;
}

@members {
private boolean allowBroken = false;
public void setAllowBroken() { allowBroken = true;}
}

NEWLINE: [\r\n];
WS: [ \t];
DASH: '-';
STAR: '*';
PIPE: '|';
NUM: [0-9]+;
WORD: [A-Za-z0-9]+;
CHARACTER: ~[\r\n>];

HEADER1: '======' ; HEADER2: '=====' ; HEADER3: '====' ; HEADER4: '===' ; HEADER5: '==' ;
LINK_START: '[['; LINK_END: ']]';
BOLD_TOKEN: '**'; ITALIC_TOKEN: '//'; UNDERLINE_TOKEN: '__'; MONOSPACE_TOKEN: '\'\'';
SUP_START_TOKEN: '<sup>'; SUP_END_TOKEN: '</sup>';
SUB_START_TOKEN: '<sub>'; SUB_END_TOKEN: '</sub>';
DEL_START_TOKEN: '<del>'; DEL_END_TOKEN: '</del>';
UNFORMAT_TOKEN: '%%';
UNFORMAT_TAG_START: '<nowiki>'; UNFORMAT_TAG_END: '</nowiki>';
FORCE_LINEBREAK: ' \\\\';
BLOCKQUOTE_START: '>';
IMG_START_TOKEN: '{{'; IMG_END_TOKEN: '}}';
MACRO_START_TOKEN: '~~MACRO~~'; MACRO_END_TOKEN: '~~/MACRO~~';
NO_TOC_TOKEN: '~~NOTOC~~'; YES_TOC_TOKEN: '~~YESTOC~~';
HIDDEN_START: '<hidden'; HIDDEN_END: '</hidden>';

// Main entry
page: (block)* EOF ;

// Main block types
block
    : header
    | row
    | just_newline
    | code_box
    | blockquote
    | hidden
    | control_row
    | horizontal_rule
    ;

// Header and text
header: WS? header_tok inner_text_ext header_tok WS* NEWLINE;
header_tok: HEADER1 | HEADER2 | HEADER3 | HEADER4 | HEADER5;

// List and row types
row: line NEWLINE;
control_row: (NO_TOC_TOKEN | YES_TOC_TOKEN | WS+)+ NEWLINE;
blockquote: BLOCKQUOTE_START+ WS? (line | WS+)? NEWLINE;
horizontal_rule: DASH DASH DASH DASH+;

// Code and newlines
code_box: WS WS+ line? NEWLINE;
just_newline: (WS? | WS WS WS+) NEWLINE;

// Links and images
link: LINK_START link_target link_display? LINK_END;
link_target: (WORD | NUM | CHARACTER | WS | ITALIC_TOKEN | DASH | UNDERLINE_TOKEN)*;
link_display: PIPE (all_char | image)*;
image: IMG_START_TOKEN inner_text+ (PIPE inner_text)? IMG_END_TOKEN;

// Macro
macro: MACRO_START_TOKEN (line_item | NEWLINE)+ MACRO_END_TOKEN;

// Hidden
hidden: HIDDEN_START hidden_attributes '>' hidden_contents HIDDEN_END;
hidden_attributes: all_char*;
hidden_contents: (header | row | line | just_newline | code_box | blockquote)*;

// List items
olist_item: WS+ DASH (image)? (inner_text | styled_span | macro)+;
ulist_item: WS+ STAR (inner_text | styled_span | macro)+;

// Text composition
inner_text: (all_char | link | broken_link | PIPE);
inner_text_ext: (all_char | link | broken_link | PIPE | line_break)+;
inner_text_nowsstart: WS? (all_char_nows | link | broken_link | PIPE);

// Lines
line_item: (inner_text | styled_span | broken_span | image | broken_image | line_break);
line: (ulist_item | olist_item) | ((image | (WS? styled_span) | (WS? broken_span) | inner_text_nowsstart | broken_image | macro | broken_macro | line_break) (line_item | macro | broken_macro)*);
line_break: FORCE_LINEBREAK;

// Styled spans (grouped)
styled_span
    : bold_span | italic_span | underline_span | monospace_span
    | sup_span | sub_span | del_span | unformat_span
    ;
bold_span: BOLD_TOKEN styled_content+? BOLD_TOKEN;
italic_span: ITALIC_TOKEN styled_content+? ITALIC_TOKEN;
underline_span: UNDERLINE_TOKEN styled_content+? UNDERLINE_TOKEN;
monospace_span: MONOSPACE_TOKEN styled_content+? MONOSPACE_TOKEN;
sup_span: SUP_START_TOKEN styled_content+? SUP_END_TOKEN;
sub_span: SUB_START_TOKEN styled_content+? SUB_END_TOKEN;
del_span: DEL_START_TOKEN styled_content+? DEL_END_TOKEN;
unformat_span: (UNFORMAT_TOKEN (styled_content | broken_span)* UNFORMAT_TOKEN)
             | (UNFORMAT_TAG_START (styled_content | broken_span)* UNFORMAT_TAG_END);

// Helper for styled spans
styled_content: all_char | link | PIPE | NEWLINE | styled_span;

// All character types
all_char: all_char_nows | DASH | STAR | WS | BLOCKQUOTE_START | NO_TOC_TOKEN | YES_TOC_TOKEN;
all_char_nows: WORD | NUM | CHARACTER | broken_header;
broken_header: { allowBroken }? header_tok;

// Broken (unclosed) spans and elements
broken_span
    : { allowBroken }? (broken_bold_span | broken_italic_span | broken_underline_span | broken_monospace_span | broken_sup | broken_sub | broken_del | broken_unformat)
    ;
broken_bold_span: BOLD_TOKEN (all_char | link | PIPE)*;
broken_italic_span: ITALIC_TOKEN (all_char | link | PIPE)*;
broken_underline_span: UNDERLINE_TOKEN (all_char | link | PIPE)*;
broken_monospace_span: MONOSPACE_TOKEN (all_char | link | PIPE)*;
broken_sup: SUP_START_TOKEN | SUP_END_TOKEN;
broken_sub: SUB_START_TOKEN | SUB_END_TOKEN;
broken_del: DEL_START_TOKEN | DEL_END_TOKEN;
broken_unformat: UNFORMAT_TOKEN | UNFORMAT_TAG_END | UNFORMAT_TAG_END;
broken_image: { allowBroken }? IMG_START_TOKEN | IMG_END_TOKEN;
broken_macro: { allowBroken }? MACRO_START_TOKEN | MACRO_END_TOKEN;
broken_link: { allowBroken }? LINK_START | LINK_END;

// "no_x" spans for styled content
no_bold_span: styled_span_except_bold;
no_italic_span: styled_span_except_italic;
no_underline_span: styled_span_except_underline;
no_unformat_span: styled_span_except_unformat;
styled_span_except_bold: italic_span | underline_span | monospace_span | sup_span | sub_span | del_span | unformat_span;
styled_span_except_italic: bold_span | underline_span | monospace_span | sup_span | sub_span | del_span | unformat_span;
styled_span_except_underline: bold_span | italic_span | monospace_span | sup_span | sub_span | del_span | unformat_span;
styled_span_except_unformat: bold_span | italic_span | monospace_span | sup_span | sub_span | del_span | underline_span;