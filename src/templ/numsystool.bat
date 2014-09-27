@echo off
rem *******************************{begin:header}******************************
rem                  spssio - http://code.google.com/p/spssio/                 
rem ***************************************************************************
rem 
rem       Java classes for reading and writing
rem       SPSS/PSPP Portable and System files
rem 
rem       Copyright (C) 2013-2014 Jani Hautamaki <jani.hautamaki@hotmail.com>
rem 
rem       Licensed under the terms of GNU General Public License v3.
rem 
rem       You should have received a copy of the GNU General Public License v3
rem       along with this program as the file LICENSE.txt; if not, please see
rem       http://www.gnu.org/licenses/gpl-3.0.html
rem 
rem ********************************{end:header}*******************************

java -cp %~dp0\..\build.dir\classes.jar; spssio.util.NumberSystemTool %*
