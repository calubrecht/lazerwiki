grammar Dokuwiki;

@header {
package us.calubrecht.lazerwiki.service.parser.doku;
}


//lexer grammar DokuLexer;

NEWLINE
    : [\r\n]
    ;


HEADER1: '======' ;
HEADER2: '=====' ;
HEADER3: '====' ;
HEADER4: '===' ;
HEADER5: '==' ;

WS: [ \t] ;

LINK_START: '[[' ;
LINK_END: ']]' ;
PIPE: '|';

STAR: '*' ;
DASH: '-' ;

WORD
    : [A-Z0-9a-z]+
    ;

CHARACTER
   : ~[\r\n]
   ;

BOLD_TOKEN: '**' ;
ITALIC_TOKEN: '//' ;
UNDERLINE_TOKEN: '__';
MONOSPACE_TOKEN: '\'\'';
SUP_START_TOKEN: '<sup>';
SUP_END_TOKEN: '</sup>';
SUB_START_TOKEN: '<sub>';
SUB_END_TOKEN: '</sub>';
DEL_START_TOKEN: '<del>';
DEL_END_TOKEN: '</del>';

IMG_START_TOKEN: '{{';
IMG_END_TOKEN: '}}';

MACRO_START_TOKEN: '~~MACRO~~' ;
MACRO_END_TOKEN: '~~/MACRO~~' ;

//parser grammar DokuParser;

//options { tokenVocab=DokuLexer; }

page
    : ( header | row | just_newline | code_box)* EOF
    ;

just_newline
  : ( NEWLINE | WS)* NEWLINE
  ;

header_tok
   : HEADER1 | HEADER2 | HEADER3 | HEADER4 | HEADER5
   ;

link_target
  : (WORD | CHARACTER | WS | ITALIC_TOKEN | DASH | UNDERLINE_TOKEN) *
  ;

link_display
  :
  PIPE (all_char | image)*
  ;

link:
 LINK_START link_target link_display? LINK_END
 ;


bold_span
  :
    BOLD_TOKEN (all_char | link | PIPE | NEWLINE | styled_span)+? BOLD_TOKEN
  ;

italic_span
  :
    ITALIC_TOKEN (all_char | link | PIPE | NEWLINE | styled_span)+? ITALIC_TOKEN
  ;

underline_span
  :
      UNDERLINE_TOKEN (all_char | link | PIPE | NEWLINE | styled_span)+? UNDERLINE_TOKEN
  ;

monospace_span
  :
      MONOSPACE_TOKEN (all_char | link | PIPE | NEWLINE | styled_span)+? MONOSPACE_TOKEN
  ;

sup_span
  :
      SUP_START_TOKEN (all_char | link | PIPE | NEWLINE | styled_span)+? SUP_END_TOKEN
  ;

sub_span
  :
      SUB_START_TOKEN (all_char | link | PIPE | NEWLINE | styled_span)+? SUB_END_TOKEN
  ;

del_span
  :
      DEL_START_TOKEN (all_char | link | PIPE | NEWLINE | styled_span)+? DEL_END_TOKEN
  ;

all_char
   : WORD | CHARACTER | WS  | DASH | STAR | header_tok
   ;

all_char_nows
   :
     WORD | CHARACTER | DASH | STAR | header_tok
   ;

broken_bold_span
   :
     BOLD_TOKEN (all_char | link | PIPE )*
   ;

broken_italic_span
   :
     ITALIC_TOKEN (all_char | link | PIPE )*
   ;

broken_underline_span
   :
     UNDERLINE_TOKEN (all_char | link | PIPE )*
   ;

broken_monospace_span
   :
     MONOSPACE_TOKEN (all_char | link | PIPE )*
   ;

broken_sup
  :
   SUP_START_TOKEN | SUP_END_TOKEN
  ;

broken_sub
  :
   SUB_START_TOKEN | SUB_END_TOKEN
  ;

broken_del
  :
   DEL_START_TOKEN | DEL_END_TOKEN
  ;

styled_span
  :
    (bold_span | italic_span | underline_span | monospace_span| sup_span| sub_span| del_span )
  ;

broken_span
 :
 broken_bold_span | broken_italic_span | broken_underline_span | broken_monospace_span | broken_sup | broken_sub | broken_del
 ;

olist_item
  :
    WS+ DASH WS* inner_text
  ;

ulist_item
  :
    WS+ STAR WS* inner_text
  ;

row:
  ( line  ) NEWLINE
  ;

code_box:
  ( WS WS line) NEWLINE
  ;


image
  : IMG_START_TOKEN inner_text (PIPE inner_text)? IMG_END_TOKEN
  ;

broken_image
  :
   IMG_START_TOKEN | IMG_END_TOKEN
  ;

macro
  :
    MACRO_START_TOKEN (line_item )+  MACRO_END_TOKEN
  ;

broken_macro
 :
   MACRO_START_TOKEN | MACRO_END_TOKEN
 ;

broken_link
  :
   LINK_START | LINK_END
  ;


inner_text
  :
    (all_char | link | broken_link | PIPE )+
  ;

inner_text_nowsstart
  :
    WS? (all_char_nows | link | broken_link |  PIPE )+
  ;

header
  : WS? header_tok inner_text header_tok WS* NEWLINE
  ;

line_item
 :
   (inner_text | styled_span | broken_span | image | broken_image |  macro | broken_macro)
;

line
  : (ulist_item | olist_item | image | styled_span | broken_span | inner_text_nowsstart | image | broken_image | macro | broken_macro ) line_item*
  ;


