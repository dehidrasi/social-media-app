package bagus2x.sosmed.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import bagus2x.sosmed.data.local.ChatLocalDataSource
import bagus2x.sosmed.data.local.KeyLocalDataSource
import bagus2x.sosmed.data.local.SosmedDatabase
import bagus2x.sosmed.data.local.entity.ChatEntity
import bagus2x.sosmed.data.local.entity.KeyEntity
import bagus2x.sosmed.data.remote.ChatRemoteDataSource
import coil.network.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class ChatRemoteMediator(
    private val keyLocalDataSource: KeyLocalDataSource,
    private val chatLocalDataSource: ChatLocalDataSource,
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val label: String,
    private val database: SosmedDatabase
) : RemoteMediator<Int, ChatEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ChatEntity>
    ): MediatorResult {
        return try {
            // The network load method takes an optional String
            // parameter. For every page after the first, pass the String
            // token returned from the previous page to let it continue
            // from where it left off. For REFRESH, pass null to load the
            // first page.
            val nextKey = when (loadType) {
                LoadType.REFRESH -> Long.MAX_VALUE
                // In this example, you never need to prepend, since REFRESH
                // will always load the first page in the list. Immediately
                // return, reporting end of pagination.
                LoadType.PREPEND -> return MediatorResult.Success(
                    endOfPaginationReached = true
                )
                // Query remoteKeyDao for the next RemoteKey.
                LoadType.APPEND -> {
                    val remoteKey = database.withTransaction {
                        keyLocalDataSource.remoteKeyByLabel(label = label)
                    }

                    // You must explicitly check if the page key is null when
                    // appending, since null is only valid for initial load.
                    // If you receive null for APPEND, that means you have
                    // reached the end of pagination and there are no more
                    // items to load.
                    if (remoteKey?.nextKey == null) {
                        return MediatorResult.Success(
                            endOfPaginationReached = true
                        )
                    }

                    remoteKey.nextKey
                }
            }

            // Suspending network load via Retrofit. This doesn't need to
            // be wrapped in a withContext(Dispatcher.IO) { ... } block
            // since Retrofit's Coroutine CallAdapter dispatches on a
            // worker thread.
            val res = chatRemoteDataSource.getChats(
                nextId = nextKey,
                limit = state.config.pageSize
            )

            // Store loaded data, and next key in transaction, so that
            // they're always consistent.
            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    keyLocalDataSource.deleteByLabel(label = label)
                    chatLocalDataSource.deleteAllChats()
                }

                // Update RemoteKey for this query.
                keyLocalDataSource.insertOrReplace(
                    KeyEntity(
                        label = label,
                        nextKey = res.lastOrNull()?.asEntity()?.lastMessageSentAt
                    )
                )

                // Insert new users into database, which invalidates the
                // current PagingData, allowing Paging to present the updates
                // in the DB.
                chatLocalDataSource.save(res.map { it.asEntity() })
            }

            MediatorResult.Success(
                endOfPaginationReached = res.lastOrNull()?.asEntity()?.id == null
            )
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}
