package online.toolboox.plugin.teamdrawer.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import online.toolboox.plugin.teamdrawer.nw.NoteService
import online.toolboox.plugin.teamdrawer.nw.RoomService
import online.toolboox.plugin.teamdrawer.nw.StrokeService
import retrofit2.Retrofit

/**
 * Team drawer service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Module
object TeamDrawerServiceModule {

    /**
     * Provides the 'note' service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideNoteService(retrofit: Retrofit): NoteService {
        return retrofit.create(NoteService::class.java)
    }

    /**
     * Provides the 'room' service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideRoomService(retrofit: Retrofit): RoomService {
        return retrofit.create(RoomService::class.java)
    }

    /**
     * Provides the 'stroke' service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideStrokeService(retrofit: Retrofit): StrokeService {
        return retrofit.create(StrokeService::class.java)
    }
}
