from django.contrib import admin
from django.urls import path, include

from rest_framework.routers import DefaultRouter

from tours.views import TourViewSet
from hotels.views import HotelViewSet
from bookings.views import BookingViewSet
from users.views import RegisterView, login_view, me
from favorites.views import FavoriteViewSet


router = DefaultRouter()

router.register(r"tours", TourViewSet)
router.register(r"hotels", HotelViewSet)
router.register(r"bookings", BookingViewSet, basename="bookings")
router.register(r"favorites", FavoriteViewSet, basename="favorites")


urlpatterns = [
    path("admin/", admin.site.urls),

    path("api/auth/register/", RegisterView.as_view()),

    path("api/auth/login/", login_view),

    path("api-auth/", include("rest_framework.urls")),

    path("api/", include(router.urls)),

    path("api/me/", me),
]