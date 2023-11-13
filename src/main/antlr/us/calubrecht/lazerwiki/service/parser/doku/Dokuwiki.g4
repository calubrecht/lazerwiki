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

IMG_START_TOKEN: '{{';
IMG_END_TOKEN: '}}';

//parser grammar DokuParser;

//options { tokenVocab=DokuLexer; }

page
    : ( header | row | just_newline )* EOF
    ;

just_newline
  : ( NEWLINE | WS)* NEWLINE
  ;

header_tok
   : HEADER1 | HEADER2 | HEADER3 | HEADER4 | HEADER5
   ;

link_target
  : (CHARACTER | WS)*
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
    BOLD_TOKEN (all_char | link | PIPE | NEWLINE)+ BOLD_TOKEN
  ;

all_char
   : WORD | CHARACTER | WS  | DASH | STAR | header_tok
   ;

broken_bold_span
   :
     BOLD_TOKEN (all_char | link | PIPE )*
   ;

broken_image
  :
   IMG_START_TOKEN  | IMG_END_TOKEN
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

inner_text
  :
    (all_char | link | PIPE )+
  ;

header
  : WS* header_tok inner_text header_tok WS* NEWLINE
  ;

image
  : IMG_START_TOKEN (WORD | WS | CHARACTER | PIPE )+ IMG_END_TOKEN
  ;


line
  : (ulist_item | olist_item | (inner_text | bold_span | broken_bold_span | image | broken_image ))+
  ;


