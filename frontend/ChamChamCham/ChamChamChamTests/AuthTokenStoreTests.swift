//
//  AuthTokenStoreTests.swift
//  ChamChamChamTests
//
//  Created by iyungui on 7/20/26.
//

import Foundation
import Testing
@testable import ChamChamCham

/// 세션 만료 스트림의 구독 정리 검증. clear()는 실제 키체인 계정을 지우지만
/// (APIClientEnvelopeTests도 실제 스토어를 쓰는 선례), 토큰을 영속하는 다른 테스트가 없다.
@Suite("AuthTokenStore 세션 만료 스트림")
struct AuthTokenStoreTests {

    /// 조건이 참이 될 때까지 최대 ~1초 폴링.
    @discardableResult
    private func waitUntil(_ condition: () async -> Bool) async -> Bool {
        for _ in 0..<500 {
            if await condition() { return true }
            try? await Task.sleep(for: .milliseconds(2))
        }
        return await condition()
    }

    @Test("소비자 Task가 끝나면 continuation이 저장소에서 제거된다")
    func terminatedConsumerIsRemoved() async {
        let store = AuthTokenStore()
        let stream = await store.sessionExpiredEvents()
        #expect(await store.sessionExpiredObserverCount == 1)

        let consumer = Task { for await _ in stream {} }
        consumer.cancel()
        await consumer.value

        // 제거는 onTermination → actor 복귀 Task를 거치므로 폴링으로 관찰한다
        #expect(await waitUntil { await store.sessionExpiredObserverCount == 0 })
    }

    @Test("구독 하나가 끝나도 남은 구독자는 clear() 이벤트를 계속 받는다")
    func remainingSubscriberStillReceivesClear() async {
        let store = AuthTokenStore()
        let kept = await store.sessionExpiredEvents()
        let dropped = await store.sessionExpiredEvents()
        #expect(await store.sessionExpiredObserverCount == 2)

        let droppedConsumer = Task { for await _ in dropped {} }
        droppedConsumer.cancel()
        await droppedConsumer.value
        #expect(await waitUntil { await store.sessionExpiredObserverCount == 1 })

        await store.clear()
        // AsyncStream은 기본 무제한 버퍼라 순회 시작 전의 yield도 보존된다
        var received = false
        for await _ in kept {
            received = true
            break
        }
        #expect(received)
    }
}
