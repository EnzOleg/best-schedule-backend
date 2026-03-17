from django.db import models
from django.conf import settings


class Favorite(models.Model):

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="favorites"
    )

    tour = models.ForeignKey(
        "tours.Tour",
        on_delete=models.CASCADE,
        null=True,
        blank=True
    )

    hotel = models.ForeignKey(
        "hotels.Hotel",
        on_delete=models.CASCADE,
        null=True,
        blank=True
    )

    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["user", "tour"],
                name="unique_user_tour_favorite"
            )
        ]