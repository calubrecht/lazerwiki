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

CHARACTER
    : ~[\r\n]
    ;

BOLD_TOKEN: '**' ;

IMG_START_TOKEN: '{{';
IMG_END_TOKEN: '}}';

//parser grammar DokuParser;

//options { tokenVocab=DokuLexer; }

page
    : (WS* header WS* NEWLINE | row | just_newline)* EOF
    ;


row:
  ( line  ) NEWLINE
  ;

just_newline
  : ( NEWLINE | WS) +
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
   : WORD | CHARACTER | WS | header_tok
   ;

broken_bold_span
   :
     BOLD_TOKEN (all_char | link | PIPE )*
   ;

broken_image
  :
   IMG_START_TOKEN  | IMG_END_TOKEN
  ;

inner_text
  :
    (all_char | link | PIPE )+
  ;

header
  : header_tok inner_text header_tok
  ;

image
  : IMG_START_TOKEN (WORD | WS | CHARACTER | PIPE )+ IMG_END_TOKEN
  ;

line
  : (inner_text | bold_span | broken_bold_span | image | broken_image)+
  ;


