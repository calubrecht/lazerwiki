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

CHARACTER
    : ~[\r\n]
    ;

BOLD_TOKEN: '**' ;
ITALIC_TOKEN: '//' ;
UNDERLINE_TOKEN: '__';
MONOSPACE_TOKEN: '\'\'';

IMG_START_TOKEN: '{{';
IMG_END_TOKEN: '}}';

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
  : (CHARACTER | WS | ITALIC_TOKEN | DASH | UNDERLINE_TOKEN) *
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

all_char
   : CHARACTER | WS  | DASH | STAR | header_tok
   ;

all_char_nows
   :
     CHARACTER | DASH | STAR | header_tok
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

styled_span
  :
    (bold_span | italic_span | underline_span | monospace_span| broken_bold_span | broken_italic_span | broken_underline_span | broken_monospace_span)
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
  : IMG_START_TOKEN inner_text IMG_END_TOKEN
  ;

broken_image
  :
   IMG_START_TOKEN | IMG_END_TOKEN
  ;


inner_text
  :
    (all_char | link | PIPE )+
  ;

inner_text_nowsstart
  :
    WS? (all_char_nows | link | PIPE )+
  ;

header
  : WS? header_tok inner_text header_tok WS* NEWLINE
  ;

line_item
 :
   (inner_text | styled_span | image | broken_image )
;

line
  : (ulist_item | olist_item | image | styled_span | inner_text_nowsstart | styled_span | image | broken_image  ) line_item*
  ;


