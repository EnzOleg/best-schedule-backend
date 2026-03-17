from django.db import models
from django.conf import settings


class Review(models.Model):

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE
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

    rating = models.IntegerField()
    text = models.TextField()

    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Review {self.id}"