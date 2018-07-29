package com.test.rx_java_test;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;
import rx.Observable;

@Log
public class OperatorsTest {
	public void zip() {
		Observable<Integer> oneToEight = Observable.range(1, 8);
		Observable<String> ranks = oneToEight.map(Object::toString);
		Observable<String> files = oneToEight
				.map( x-> 'a' + x-1)
				.map(asc -> (char)asc.intValue())
				.map(ch -> Character.toString(ch));
		
		Observable<String> squares = files.flatMap(file -> ranks.map(r -> file+r));
		squares.subscribe(System.out::println, e -> e.printStackTrace(), () -> System.out.println("complete"));
	}
	
	public void zip2() {
		Observable<LocalDate> nextTenDays = Observable.range(1,  10)
				.map(i -> LocalDate.now().plusDays(i));
		Observable<Vacation> possibleVacations = Observable.just(City.Warsaw, City.London, City.Paris)
				.flatMap( city -> nextTenDays.map(date -> new Vacation(city, date))
						.flatMap( vacation -> Observable.zip(
								vacation.weather().filter(Weather::isSunny), // isSunny 대상이 아닌 경우 결과값이서 무시됨
								vacation.cheapFlightFrom(City.NewYork),
								vacation.cheapHotel(),
								(w,f,h) -> vacation)
								));
		possibleVacations.subscribe(
				v -> System.out.println(v.where.getName() + ", " + v.when.toString()), 
				e -> e.printStackTrace(), 
				() -> System.out.println("complete")
				);
	}
	
	public void zipProblem() throws InterruptedException {
		//두 공급자는 동일한 주기로 스트림을 생성 
		Observable<Long> red = Observable.interval(10,  TimeUnit.MILLISECONDS);
		Observable<Long> green = Observable.interval(10,  TimeUnit.MILLISECONDS);
		Observable.zip(
				red.timestamp(), 
				green.timestamp(),
				(r, g) -> r.getTimestampMillis() - g.getTimestampMillis()
				).forEach(System.out::println);
		
		Thread.sleep(1000);
		
		// 하지만 두 스트림이 생성되는 시간차가 크게 존재한다면  (green을 100으로 늘리면 스트림을 생성할 때마다 시간차가 증가한다)
		// zip을 사용하면 이런 스트림의 시간차 때문에 메모리 누수가 있을 수 있다. 
	}
	
	public void combinelast() throws InterruptedException {
		// 아래 combineLatest는 zip과 달리 두 스트림간의 출력의 시간차가 일정하다. 
		// 하지만 방출시 짝이 반드시 이뤄진다는 보장은 없고 짝을 이룰수 있으면 최소한 시간차로 방출을 하게 된다. 
		Observable.combineLatest(
				Observable.interval(17, TimeUnit.MILLISECONDS).map(x -> "S" + x),
				Observable.interval(10, TimeUnit.MILLISECONDS).map(x -> "F" + x),
				(s,f) -> f + ":" + s).forEach(System.out::println);
		
		Thread.sleep(1000);
	}
	
	public void withLatestFrom() throws InterruptedException {
		// 한쪽 스트림에 새로운 값이 나타날 경우만 묶어 방출하고 싶을 경우. slow가 주체가 되어 방출할 때만 fast의 값이 묶여  같이 방출. 그외 fast의 값은 무시 
		Observable<String> fast = Observable.interval(10, TimeUnit.MILLISECONDS).map(x -> "S" + x).startWith("FX");
		Observable<String> slow = Observable.interval(20, TimeUnit.MILLISECONDS).map(x -> "F" + x);
		slow.withLatestFrom(fast, (s,f) -> s + ":" + f).forEach(s -> log.info(s+""));
		
		// 시작값을 지정
		Observable.just(1,2).startWith(0).forEach(s -> log.info(s+""));
		Thread.sleep(1000);
	}
	
	@Test
	public void scan() throws InterruptedException {
		Observable<Integer> progress = Observable.just(10,14,12,14,16);
		// 점진적 합산으로 나온 스트림을 리턴
		Observable<Integer> totalProgress = progress.scan((total, chunk) -> total + chunk);
		totalProgress.forEach(s -> log.info(s+""));
		// 팩토리얼 계산의 예제. BigInteger.ONE은 누산기의 초기값
		Observable<BigInteger> factorials = Observable.range(2, 10).scan(BigInteger.ONE, (big, cur) -> big.multiply(BigInteger.valueOf(cur)));
		factorials.forEach(s -> log.info(s+""));
		Thread.sleep(1000);
	}
	
	public void reduce() {
		// 최종 합산된 결과만 필요할 경우
		Observable<Integer> progress = Observable.just(10,14,12,14,16);
		progress.reduce((a,b) -> a + b).forEach(s -> log.info(s+""));
	}
	
	@Test 
	public void collect() {
		Observable<List<Integer>> all = Observable.range(10, 20).collect(ArrayList::new, List::add);
	}
	
	@Test
	public void concatFirst() {
		// concat은 List<T>의 작동방식과 유사 . 순차적 결합이며 결합 방향은 왼쪽에서 오른쪽
		Observable<String> fromCache = Observable.just("cache");
		Observable<String> fromDB = Observable.just("db");
		// 결합하여 둘 중 결과가 있는 처음의 것을 가져옴
		Observable<String> found = Observable.concat(fromCache, fromDB).first();
	}
	
	@Test
	public void complicatedExample() {

	}
	
	Observable<Object> speak(String quote, long milisPerChar) {
		String[] tokens = quote.replaceAll("[:,]", "").split(" ");
		Observable<String> words = Observable.from(tokens);
		Observable<Long> absoluteDelay = words.map(String::length)
				.map(len -> len*milisPerChar)
				.scan((total, current) -> total + current);
		return words.zipWith(absoluteDelay.startWith(0L), Pair::of);
	}
	
	
	@Getter
	@AllArgsConstructor
	class Vacation {
		private final City where;
		private final LocalDate when;
		public Observable<Weather> weather() {
			return Observable.just(new Weather());
		}
		
		public Observable<Flight> cheapFlightFrom(City from) {
			return Observable.just(new Flight());
		}
		
		public Observable<Hotel> cheapHotel() {
			return Observable.just(new Hotel());
		}
	}
	
	@Getter
	static class City {
		private String name;
		public City(String name) {
			this.name = name;
		}
		public static final City Warsaw = new City("Warsaw");
		public static final City London = new City("London");
		public static final City Paris = new City("Paris");
		public static final City NewYork = new City("NewYork");
	}
	
	class Flight {}
	
	class Weather {
		public boolean isSunny(){
				return true;
		}
		
	}
	
	class Hotel {}
}
