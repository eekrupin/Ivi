package ru.ekrupin.ivi.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import ru.ekrupin.ivi.data.sync.RoomSyncPushApplier
import ru.ekrupin.ivi.data.sync.RoomSyncSnapshotStore
import ru.ekrupin.ivi.data.sync.RoomSyncStateStore
import ru.ekrupin.ivi.data.sync.SyncCoordinator
import ru.ekrupin.ivi.data.sync.SyncEngine
import ru.ekrupin.ivi.data.sync.SyncPushApplier
import ru.ekrupin.ivi.data.sync.SyncSnapshotStore
import ru.ekrupin.ivi.data.sync.SyncStateStore
import ru.ekrupin.ivi.data.sync.remote.OkHttpSyncRemoteDataSource
import ru.ekrupin.ivi.data.sync.remote.SyncRemoteDataSource

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    fun provideSyncRemoteDataSource(okHttpClient: OkHttpClient): SyncRemoteDataSource =
        OkHttpSyncRemoteDataSource(okHttpClient)

    @Provides
    fun provideSyncSnapshotStore(store: RoomSyncSnapshotStore): SyncSnapshotStore = store

    @Provides
    fun provideSyncStateStore(store: RoomSyncStateStore): SyncStateStore = store

    @Provides
    fun provideSyncPushApplier(applier: RoomSyncPushApplier): SyncPushApplier = applier

    @Provides
    fun provideSyncEngine(coordinator: SyncCoordinator): SyncEngine = coordinator
}
