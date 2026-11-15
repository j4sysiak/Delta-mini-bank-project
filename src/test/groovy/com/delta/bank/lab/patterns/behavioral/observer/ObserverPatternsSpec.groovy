package com.delta.bank.lab.patterns.behavioral.observer

import com.delta.bank.lab.patterns.behavioral.BalanceEvent
import com.delta.bank.lab.patterns.behavioral.ObservableAccount
import spock.lang.Specification

class ObserverPatternsSpec  extends Specification {

    /*
Observer to wzorzec, w którym jeden obiekt (obserwowany) powiadamia zainteresowane obiekty (obserwatorów) o zmianach,
bez ścisłego uzależniania się od nich.
W tym przykładzie `ObservableAccount` jest obserwowanym kontem, a funkcje przekazane do `subscribeEvent` — obserwatorami.
Po wpłacie lub wypłacie konto publikuje zdarzenie, np. `BalanceEvent.Deposited` albo `BalanceEvent.Withdrawn`.

    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
        publish(new BalanceEvent.Deposited(id, amount));
    }

Testy sprawdzają, że zdarzenia przychodzą we właściwej kolejności, można przestać je otrzymywać przez `unsubscribe`,
a nieudana wypłata nie wysyła powiadomienia.
Dzięki temu konto nie musi wiedzieć, co robią jego obserwatorzy
— mogą np. aktualizować historię lub wysyłać powiadomienia.
Ostatni test sprawdza jedynie typ zdarzenia przez `instanceof`; nie testuje dopasowania wzorcem `switch`, mimo nazwy.
    * */


    // Konto (ObservableAccount) publikuje zdarzenie po udanej wpłacie lub wypłacie
    //    — test pokazuje też, że nieudana wypłata nie publikuje powiadomienia.
    // Test weryfikuje kolejność typów zdarzeń i kwotę wpłaty, ale nie sprawdza kwoty zdarzenia wypłaty.
    def "obserwatorzy otrzymują zdarzenia we właściwej kolejności od obiektu (obserwowanego)"() {
        given:
        def account = new ObservableAccount("PLN-1")
        def eventsList = []
        // eventsList << t oznacza:
        // dopisz t na końcu listy eventsList.
        // W Groovy operator << dla list działa jak eventsList.add(t) — lista zostaje zmieniona, a wcześniejsze elementy pozostają.
        //   Tutaj t to zdarzenie przekazane obserwatorowi, np. BalanceEvent.Deposited.
        //   Po kolejnych powiadomieniach lista zbiera zdarzenia w kolejności ich otrzymania.

        // `{ event -> eventsList << event }` to **closure w Groovy** przekazywany do `subscribe`.
        // `event` jest jej argumentem, a `eventsList << event` dopisuje zdarzenie do listy.
        // Jeśli `subscribe` przyjmuje `Consumer`, Groovy może automatycznie dopasować closure do tego interfejsu.
        account.subscribeEvent { event -> eventsList << event }

        /*
        @FunctionalInterface
        public interface Consumer<T> {
          void accept(T t);
        }
        Consumer<T> consumer = new Consumer<T>() {
            @Override
            public void accept(T t) {
                events << t
            }
        }

        Consumer<T> consumerLambda = (T t) -> events << t

        W Groovy można użyć skróconej składni closure:
        account.subscribe { event -> events << event }
        account.subscribe { events << it } // it to domyślny argument closure,
        jeśli nie podano nazwy argumentu.

        * */

        when:
        account.deposit(new BigDecimal("50.00"))
        account.withdraw(new BigDecimal("20.00"))

        then:
        events*.getClass() == [BalanceEvent.Deposited, BalanceEvent.Withdrawn]
        events[0].amount() == new BigDecimal("50.00")
        account.balance() == new BigDecimal("30.00")
    }

    def "wyrejestrowanie zatrzymuje powiadomienia"() {
        given:
        def account = new ObservableAccount("PLN-1")
        def count = 0
        def unsubscribe = account.subscribe { count++ }

        when:
        account.deposit(new BigDecimal("1.00"))
        unsubscribe.run()
        account.deposit(new BigDecimal("1.00"))

        then:
        count == 1
    }

    def "nieudana wypłata nie publikuje nic"() {
        given:
        def account = new ObservableAccount("PLN-1")
        def listener = Mock(java.util.function.Consumer)
        account.subscribe(listener)

        when:
        account.withdraw(new BigDecimal("1.00"))

        then:
        thrown(IllegalStateException)
        0 * listener.accept(_)
    }

    // Sprawdza `instanceof`, a nie dopasowanie przez `switch`.
    def "pattern matching switch handles events"() {
        given:
        BalanceEvent event = new BalanceEvent.Deposited("PLN-1", BigDecimal.TEN)

        expect:
        event instanceof BalanceEvent.Deposited
    }
}
