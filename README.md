可用的指令:

目前指令寫在 runFile.txt 裡, 使用前需要注意一下自己 Chrome 的版本與 pom.xml 裡的 driver 版本是否相容.

開啟某頁 <br>
goPage -p { 網址 } <br>

重整頁面 <br>
refresh 

點擊元素 <br>
click by selector ( className/cssSelector/id/linkText/name/partialLinkText/tagName/xpath ) <br>
click -b { 選擇器種類 } -s { 選擇器的值 }

等待頁面 <br>
wait page <br>
wait -p { 網址 } optional: [ -r 等待網頁 ready / -l 等待網頁離開 ]

等待元素 <br>
wait element by selector ( className/cssSelector/id/linkText/name/partialLinkText/tagName/xpath ) on condition ( clickable/visible/exist ) <br>
wait -b { 選擇器種類 } -s { 選擇器的值 } -c { 條件的值 }

欄位設值 <br>
set field by ( className/cssSelector/id/linkText/name/partialLinkText/tagName/xpath ) with value <br>
set field -b { 選擇器種類 } -s { 選擇器的值 } -v { 設定的值 }

Selenium 線程睡眠 <br>
sleep in milliseconds <br>
sleep -t { 毫秒數 / 若沒有就是預設 1000 毫秒 }
