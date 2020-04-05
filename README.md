# language-interpreter

В этом проекте лежит решение задания для проекта стажировки IDE Feature Suggester в JetBrains

main функция лежит в src/main/java/text_editor.java, запускать проще всего через IDE

При запуске открывается два окна, одно из них предзначенно для написания кода, другое для вывода результатов работы.

В окне для написания есть кнопка Run. При её нажатии и условии, что код не содержит синтаксических ошибок и исполнение также завершилось без ошибок (деление на ноль, использование необъявленных переменных), результат работы программы будет отображен во втором окне.

В случае обнаружения нового if, содержащего хотя бы два Statement, вывод программы заменяется сообщением "New if detected".

Также у решения присутствуют тесты (src/test/kotlin/*), в них вывод в окно подменяется записью в строку, для удобства такого тестирования используется класс Writer, инкапсулирующий в себе логику записи куда-либо.

" Немного о реализации:
@Для каждой сущности создается класс, каждый из которых наследуется от базового. У классов есть статический метод parse, который пытается прочитать соответствущую ему сущность (Число, if, сравнение, арифметические операции и т д).

В итоге получается дерево Statement'ов - самма программа. из корня запускатеся виртуальный метод eval, который рекурсивно обходит дерево и обрабатывает все объекты.

Описание задания:

Грамматика модельного языка
Program → StatementList

StatementList → empty | StatementList Statement

Statement → ExpressionStatement | IfStatement | AssignStatement | BlockStatement

ExpressionStatement -> Expression ;

IfStatement → if ( Expression ) Statement

AssignStatement → @ Identifier = Expression ;

BlockStatement → { StatementList }

Expression → ConditionExpression

ConditionExpression -> PlusMinusExpression | PlusMinusExpression < PlusMinusExpression | PlusMinusExpression > PlusMinusExpression

PlusMinusExpression → MultiplyDivisionExpression | PlusMinusExpression + MultiplyDivisionExpression | PlusMinusExpression - MultiplyDivisionExpression

MultiplyDivisionExpression → SimpleExpression | MultiplyDivisionExpression * SimpleExpression | MultiplyDivisionExpression / SimpleExpression

SimpleExpression → Identifier | Integer | ( Expression )

Identifier → Идентификатор из латинских букв

Integer → Целое число, помещающееся в тип int в Java

Между любыми лексемами программы могут быть пробельные символы (собственно пробелы, табуляции, переносы строк).

Особенности семантики языка
Если Statement является ExpressionStatement’ом, то результат выражения идёт в вывод программы на этом языке.

Результаты всех выражений имеют тип int

Тело if’а выполняется, если его условие не равно 0.

Операторы сравнения < и > возвращают 1, если соотношение истинно и 0, если ложно.

Пример программы модельного языка:

@x  = 10;
@second = 20;
if (second - 19) {
  x + 1;
}
x*second + second/x*3;
У этой программы должен быть вывод 11, 206

Задача
Требуется реализовать простой редактор (желательно на стандартной Java библиотеке Swing, языком программирование может быть как Java, так и Kotlin). В редакторе должна присутствовать кнопка запуска, после чего пользователь должен увидеть в каком-то виде вывод программы. В процессе редактирования кода в Вашем редакторе, нужно уметь отлавливать ситуацию, когда пользователь оборачивает if’ом (возможно ещё без условия) и фигурными скобками несколько Statement’ов (в случае, если программа корректна). Об этом событии нужно сообщать пользователю.

Дизайн и функциональность самого редактора будут оцениваться в последнюю очередь, не стоит на это тратить время.

Пример редактирования
Было

10 * 2;
13 + 7;
Затем пользователь дописал if() в начало (мог написать и с условием сразу) :

if () 10 * 2;
13 + 7;
Пока ничего сообщать не нужно. Затем пользователь расставил скобки (в любом порядке)

if () { 10 * 2;
13 + 7; }
В этот момент надо сообщить пользователю, что добавился обрамляющий if.

Рекомендации
Для ускорения выполнения задания рекомендую найти на просторах интернета пример какого-нибудь простенького редактора (буквально в один файл) и адаптировать его.

Рекомендую транслировать модельную программу в абстрактное синтаксическое дерево. На основе сравнения этих деревьев будет легко определить, был ли добавлен обрамляющий if или произошло какое-то другое изменение.

Трансляцию модельной программы рекомендую реализовать вручную (без помощи генераторов грамматик) посредством простого рекурсивного спуска (левая рекурсия легко разворачивается в цикл в коде парсера).

