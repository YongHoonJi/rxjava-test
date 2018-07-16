package com.test.rx_java_test;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rx.Observable;

public class MonoTest {
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
	
	@Test
	public void combinelast() throws InterruptedException {
		// 아래 combineLatest는 zip과 달리 두 스트림간의 출력의 시간차가 일정하다. 
		// 하지만 방출시 짝이 반드시 이뤄진다는 보장은 없고 짝을 이룰수 있으면 최소한 시간차로 방출을 하게 된다. 
		Observable.combineLatest(
				Observable.interval(17, TimeUnit.MILLISECONDS).map(x -> "S" + x),
				Observable.interval(10, TimeUnit.MILLISECONDS).map(x -> "F" + x),
				(s,f) -> f + ":" + s).forEach(System.out::println);
		
		Thread.sleep(1000);
	}
	
	@Test
	public void withLatestFrom() {
		
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
